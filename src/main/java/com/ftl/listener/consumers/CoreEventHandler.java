//package com.ftl.listener.consumers;
//
//import com.fasterxml.jackson.databind.MapperFeature;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.ftl.common.kafka.KafkaProducer;
//import com.ftl.common.kafka.KafkaRequestHandler;
//import com.ftl.common.kafka.ThreadedKafkaConsumer;
//import com.ftl.common.model.Message;
//import com.ftl.common.model.notify.NotificationReq;
//import com.ftl.common.model.notify.ScSendReq;
//import com.ftl.listener.configurations.AppConf;
//import com.ftl.listener.model.AccEvent;
//import com.ftl.listener.model.AssetChangeEvent;
//import com.ftl.listener.model.CashEvent;
//import com.ftl.listener.model.MessageSocket;
//import com.ftl.listener.model.OrderEvent;
//import com.ftl.listener.producers.RequestSender;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Properties;
//
//@Service
//@Slf4j
//public class CoreEventHandler extends KafkaRequestHandler {
//
//
//    private final RequestSender requestSender;
//    private final ObjectMapper om;
//    private final AppConf appConf;
//
//
//    @Autowired
//    public CoreEventHandler(
//            ObjectMapper objectMapper,
//            AppConf appConf,
//            RequestSender requestSender
//    ) {
//        super();
//        this.requestSender = requestSender;
//        this.om = objectMapper;
//        this.appConf = appConf;
//
//        if (!"socket".equals(appConf.getCoreChannel()))
//            this.init(objectMapper, appConf.getKafkaBootstraps(), appConf.getClusterId(), List.of(appConf.getTopics().getCoreEvent()), 1, null);
//    }
//
//    protected void init(ObjectMapper om, String bootStrapServer, String clusterId, List<String> topics, int maxThread, KafkaProducer<String, String> producer) {
//        log.info("Init kafka event listener with topics {}", topics);
//        this.producer = producer == null ? new KafkaProducer<>(bootStrapServer, null, new Properties()) : producer;
//        this.sendOut = pair -> {
//        };
//        this.handler = record -> {
//            try {
//                String message = record.value();
//
//                handleMessage(message);
//            } catch (Exception e) {
//                log.error("fail to handle message {}", record, e);
//            } finally {
//            }
//        };
//        List<String> consumeTopics = topics == null ? Arrays.asList(clusterId) : topics;
//        Properties prop = new Properties();
//        this.consumer = new ThreadedKafkaConsumer<>(bootStrapServer, clusterId, consumeTopics, prop, handler, maxThread);
//    }
//
//
//    void handleMessage(String message) {
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
//        try {
//            log.info("receive from server: {}", message);
//            MessageSocket messageSocket = objectMapper.readValue(message, MessageSocket.class);
//            requestSender.publishUserEvent(messageSocket);
//            log.info("messageJson: {}", messageSocket);
//            String tradeType = StringUtils.trim(messageSocket.getTrade_Type());
//            if ("OD".equals(tradeType)) {
//                log.info("OD, order event");
//                OrderEvent orderEvent = OrderEvent.from(messageSocket);
//                log.info("orderEvent: {}", orderEvent);
//                requestSender.sendNotification(orderEvent.toNotificationReq());
//            } else if ("CI".equals(tradeType)) {
//                log.info("CI, cash event");
//                String tltxcd = messageSocket.getTltxcd().trim().toUpperCase();
//                if ("CIMAST".equals(tltxcd) || "SEMAST".equals(tltxcd)) {
//                    AssetChangeEvent assetChangeEvent = AssetChangeEvent.from(messageSocket);
//                    log.info("assetChangeEvent: {}", assetChangeEvent);
//                    requestSender.sendNotification(assetChangeEvent.toNotificationReq());
//                } else {
//                    CashEvent cashEvent = CashEvent.from(messageSocket);
//                    log.info("cashEvent: {}", cashEvent);
//                    requestSender.sendNotification(cashEvent.toNotificationReq());
//                }
//            } else if ("VPB".equals(tradeType)) {
//                log.info("VPB, vpbank event");
//                String tltxcd = messageSocket.getTltxcd().trim().toUpperCase();
//
//                Map<String, Object> params = new HashMap<>();
//                params.put("status", messageSocket.getMsgValue());
//
//                String method = "";
//                if ("VPB_OAUTH_STATUS".equals(tltxcd)) {
//                    method = "VPBANK_OAUTH_STATUS";
//                    params.put("content", messageSocket.getMsgContent());
//                    params.put("contentEn", messageSocket.getMsgContent_En());
//                } else if ("DELEGATION".equals(tltxcd) || "PAYMENT".equals(tltxcd)) {
//                    method = "VPBANK_OTP_STATUS";
//                    params.put("type", tltxcd);
//                }
//
//                NotificationReq notificationReq = new NotificationReq();
//                notificationReq.setMethod(method);
//                notificationReq.setPersistent(false);
//                notificationReq.setAccountNumber(messageSocket.getCustid());
//                notificationReq.setCategory("VPB_EVENT");
//
//                ScSendReq socketCluster = new ScSendReq();
//                socketCluster.setData(params);
//                notificationReq.setSocketCluster(socketCluster);
//
//                requestSender.sendNotification(notificationReq);
//            } else if ("SE".equals(tradeType)
//                    || "LN".equals(tradeType)
//                    || "CA".equals(tradeType)
//                    || "AF".equals(tradeType)
//                    || "CF".equals(tradeType)
//                    || "SO".equals(tradeType)) {
//
//                log.info("event other: {}", tradeType);
//
//                if ("AF".equals(tradeType) && "CHANGEPRICE".equalsIgnoreCase(messageSocket.getTltxcd())) {
//                    requestSender.sendUpdateExpectPriceEvent(messageSocket);
//                    return;
//                }
//                if ("AF".equals(tradeType) && ("EOD_END".equalsIgnoreCase(messageSocket.getTltxcd()) || "EOD_START".equalsIgnoreCase(messageSocket.getTltxcd()))) {
//                    requestSender.sendUpdateTransfer(messageSocket);
//                    return;
//                }
//                AccEvent accEvent = AccEvent.from(messageSocket);
//                log.info("accEvent: {}", accEvent);
//
//                String tltxcd = messageSocket.getTltxcd().trim().toUpperCase();
//
//                boolean persistent = !"AFMAST".equals(tltxcd);
//                requestSender.sendNotification(accEvent.toNotificationReq(tradeType + "_UPDATE", persistent));
//
//                if ("AFMAST".equals(tltxcd) || "REGISTCA".equals(tltxcd) || "DELETECA".equals(tltxcd)) {
//                    Map<String, Object> content = new HashMap<>();
//                    content.put("accountNumber", StringUtils.trim(messageSocket.getCustid()));
//                    content.put("subNumber", StringUtils.trim(messageSocket.getAfacctno()));
//                    requestSender.sendEvent("account-event", tltxcd, content);
//                }
//            }
//        } catch (Exception ex) {
//            log.error("error while handle message: {}, ex: {}", message, ex);
//        }
//    }
//
//
//    @Override
//    protected Object handle(Message message) throws Exception {
//        return null;
//    }
//}
