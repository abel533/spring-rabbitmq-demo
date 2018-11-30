package rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import rabbitmq.listener.User;

import java.util.Date;
import java.util.Random;

public class TestAmqp {
    private static final Logger logger = LoggerFactory.getLogger(TestAmqp.class);

    public static void main(String[] args) throws InterruptedException {
        ApplicationContext context = new GenericXmlApplicationContext("classpath:/META-INF/spring/spring-rabbitmq.xml");
        final AmqpTemplate template = context.getBean(AmqpTemplate.class);
        AmqpAdmin admin = context.getBean(AmqpAdmin.class);
        //定义交换机
        Exchange exchange = ExchangeBuilder.topicExchange("logger").durable(true).build();
        admin.declareExchange(exchange);

        String[] keys = new String[]{"logger.error", "logger.warn", "logger.info"};
        //测试发送数据
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            String key = keys[random.nextInt(3)];
            String message = key + " > " + i + " " + new Date();
            User obj = new User(message, i);
            template.convertAndSend("logger", key, obj);
            logger.info("[Send] " + obj);
            Thread.sleep(random.nextInt(5000) + 1000);
        }
    }

}
