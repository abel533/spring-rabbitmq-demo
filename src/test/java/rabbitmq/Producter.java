package rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rabbitmq.listener.User;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author liuzh
 */
@Component
public class Producter {
    public static final Logger logger = LoggerFactory.getLogger(Producter.class);

    @Autowired
    private AmqpTemplate template;

    @Autowired
    private AmqpAdmin admin;

    @PostConstruct
    protected void init() {
        //定义交换机
        Exchange exchange = ExchangeBuilder.topicExchange("logger").durable(true).build();
        admin.declareExchange(exchange);
        //还可以定义队列和绑定
    }

    final Random random = new Random();
    final String[] keys = new String[]{"logger.error", "logger.warn", "logger.info"};
    AtomicInteger count = new AtomicInteger();

    @Scheduled(fixedDelay = 1000)
    protected void product() {
        String key = keys[random.nextInt(3)];
        int i = count.getAndIncrement();
        String message = key + " > " + i + " " + new Date();
        User obj = new User(message, i);
        template.convertAndSend("logger", key, obj);
        logger.info("[Send] " + obj);
    }

}
