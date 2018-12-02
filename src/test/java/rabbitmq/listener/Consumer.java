package rabbitmq.listener;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class Consumer {
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

    /**
     * 接收对象的例子
     *
     * 该方法还可以直接注入 org.springframework.amqp.core.Message 对象
     *
     * @param data
     * @param deliveryTag
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "logger.all", durable = "true"),
            exchange = @Exchange(value = "logger", durable = "true", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = "logger.#"
    ))
    public void all(User data, @Header(AmqpHeaders.DELIVERY_TAG) Long deliveryTag, Channel channel) {
        try {
            //测试用，随机确认和拒绝(并返回队列)
            if(Math.random() > 0.5d){
                logger.info("[reject] deliveryTag:" + deliveryTag + ", message: " + data);
                channel.basicReject(deliveryTag, true);
            } else {
                logger.info("[ack   ] deliveryTag:" + deliveryTag + ", message: " + data);
                channel.basicAck(deliveryTag, false);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 指定名的队列
     *
     * @param message
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "logger.error", durable = "true"),
            exchange = @Exchange(value = "logger", durable = "true", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = "logger.error"
    ))
    public void error(Message message){
        logger.info("[logger.error] " + new String(message.getBody()));
    }


    /**
     * 匿名队列，独占访问，自动删除
     *
     * @param data
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue,
            exchange = @Exchange(value = "logger", durable = "true", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = "logger.info"
    ))
    public void info(User data){
        logger.info("[logger.info ] " + data);
    }

}
