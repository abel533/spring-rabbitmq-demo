本文从安装和配置 RabbitMQ 开始，准备好环境后，直接在 Spring 中集成，并且针对 Spring 中的常见用法提供了示例和讲解。

## 安装

一般开发环境可能用的都是 Windows，生产环境 Linux 用的比较多，这里针对 Windows 和 Ubuntu 的安装说明简单提炼。其他环境可以直接参考官方文档：https://www.rabbitmq.com/download.html

### Windows 安装

Windows 上安装很容易，先安装 Erlang/OTP 环境（注意和 RabbitMQ 版本匹配），再安装 RabbitMQ 即可。

下载地址：
- 版本依赖: https://www.rabbitmq.com/which-erlang.html
- Erlang/OTP: http://www.erlang.org/downloads
- RabbitMQ: https://www.rabbitmq.com/install-windows.html

### Ubuntu 安装

1 为了使用存储库方式安装最新版本，需要将 RabbitMQ 签名秘钥添加到 `apt-key` 中，从下面**两种方式选择一种**方式执行：
```bash
sudo apt-key adv --keyserver "hkps.pool.sks-keyservers.net" --recv-keys "0x6B73A36E6026DFCA"
```
**或者**
```bash
wget -O - "https://github.com/rabbitmq/signing-keys/releases/download/2.0/rabbitmq-release-signing-key.asc" | sudo apt-key add -
```
>第二种方式无需密钥服务器即可下载和导入密钥。
>我使用的第一种。

2 然后在 [packagecloud](https://packagecloud.io/rabbitmq/rabbitmq-server/install#bash-deb) 有段脚本（自动根据服务器版本选择对应的安装源），当前(2018-11-30)的内容如下：
```bash
curl -s https://packagecloud.io/install/repositories/rabbitmq/rabbitmq-server/script.deb.sh | sudo bash
```

3 执行该脚本后，继续然后执行下面的命令：
```bash
sudo apt-get update
```
更新后，可以通过下面命令查看当前的 rabbitmq-server 的可用版本:
```bash
apt-cache madison rabbitmq-server
```
我这里的结果(2018-12-01)显示如下：
```
rabbitmq-server |    3.7.9-1 | https://packagecloud.io/rabbitmq/rabbitmq-server/ubuntu bionic/main amd64 Packages
rabbitmq-server |    3.7.8-1 | https://packagecloud.io/rabbitmq/rabbitmq-server/ubuntu bionic/main amd64 Packages
rabbitmq-server |    3.7.7-1 | https://packagecloud.io/rabbitmq/rabbitmq-server/ubuntu bionic/main amd64 Packages
rabbitmq-server |    3.7.6-1 | https://packagecloud.io/rabbitmq/rabbitmq-server/ubuntu bionic/main amd64 Packages
rabbitmq-server |    3.7.5-1 | https://packagecloud.io/rabbitmq/rabbitmq-server/ubuntu bionic/main amd64 Packages
rabbitmq-server |   3.6.16-2 | https://packagecloud.io/rabbitmq/rabbitmq-server/ubuntu bionic/main amd64 Packages
rabbitmq-server |   3.6.16-1 | https://packagecloud.io/rabbitmq/rabbitmq-server/ubuntu bionic/main amd64 Packages
rabbitmq-server |   3.6.15-1 | https://packagecloud.io/rabbitmq/rabbitmq-server/ubuntu bionic/main amd64 Packages
rabbitmq-server |   3.6.10-1 | http://archive.ubuntu.com/ubuntu bionic/main amd64 Packages
```

4 执行下面的命令安装 rabbitmq-server
```bash
sudo apt-get install rabbitmq-server
```
此时安装的应该是最新的版本。

>可以通过 sudo apt-get install rabbitmq-server=3.7.9-1 安装指定版本。

## 配置

接下来主要是在 Ubuntu 环境（Windows 环境类似）进行配置。由于没有桌面环境，因此先通过命令创建可以外网访问 rabbitmq 的用户，然后启用 management 在通过网页进行管理。

添加用户 root，密码 root。（**根据自己需要设置**）
```bash
sudo rabbitmqctl add_user root root
```

给 root 添加管理权限。
```bash
sudo rabbitmqctl set_user_tags root administrator
```

给 root 添加默认虚拟主机的所有权限。
```bash
sudo rabbitmqctl set_permissions -p / root ".*" ".*" ".*"
```

>**Windows 中的操作过程**
>```
>D:\Program Files\RabbitMQ Server\rabbitmq_server-3.7.9\sbin>rabbitmqctl.bat add_user root root
>Adding user "root" ...
>
>D:\Program Files\RabbitMQ Server\rabbitmq_server-3.7.9\sbin>rabbitmqctl.bat set_user_tags root administrator
>Setting tags for user "root" to [administrator] ...
>
>D:\Program Files\RabbitMQ Server\rabbitmq_server-3.7.9\sbin>rabbitmqctl.bat set_permissions -p / root ".*" ".*" ".*"
>Setting permissions for user "root" in vhost "/" ...
>```

启用 `rabbitmq_management`
```bash
sudo rabbitmq-plugins enable rabbitmq_management
```
>启用 `rabbitmq_management` 后不需要重启服务

此后可以直接访问 rabbitmq 的 http://RabbitMQ服务IP:15672 通过 WEB 进行管理。

>**备忘录**（暂时不用关注这里，测试集群时可用）
>单机启动多个带有 rabbitmq_management 节点时的配置
>```
>RABBITMQ_NODE_PORT=5672 RABBITMQ_NODENAME=rabbitl RABBITMQ_SERVER_START_ARGS="-rabbitmq_management listener [{port , 156721}]" rabbitmq-server -detached
>```
>参考 **RabbitMQ实战指南** 7.1.5 单机多节点配置

准备好 RabbitMQ 环境后，下面直接和 Spring 集成。

>初学者建议先通过官方示例了解 RabbitMQ 的基本概念和用法：https://www.rabbitmq.com/getstarted.html

## Spring 集成

下面先是 Spring 集成的配置，然后是项目中具体的用法。

>**下面示例所有链接都可以直接打开展示完整内容。**
>完整示例地址：https://github.com/abel533/spring-rabbitmq-demo

### 配置

1 添加[相关依赖](https://github.com/abel533/spring-rabbitmq-demo/blob/master/pom.xml)
```xml
<dependency>
    <groupId>org.springframework.amqp</groupId>
    <artifactId>spring-rabbit</artifactId>
    <version>1.7.11.RELEASE</version>
</dependency>
<!-- spring-rabbit 依赖 spring-amqp，下面这个依赖可以不显示引入 -->
<dependency>
    <groupId>org.springframework.amqp</groupId>
    <artifactId>spring-amqp</artifactId>
    <version>1.7.11.RELEASE</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.8.11.1</version>
</dependency>
<dependency>
    <groupId>org.codehaus.jackson</groupId>
    <artifactId>jackson-mapper-asl</artifactId>
    <version>1.9.13</version>
</dependency>
```

2 配置文件

将 [spring-rabbit 配置](https://github.com/abel533/spring-rabbitmq-demo/blob/master/src/main/resources/META-INF/spring/spring-rabbitmq.xml)单独放在一个文件中，需要的时候可以直接在 Spring 中 `<import>`。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns="http://www.springframework.org/schema/beans"
       xmlns:rabbit="http://www.springframework.org/schema/rabbit"
       xmlns:task="http://www.springframework.org/schema/task"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/rabbit
       http://www.springframework.org/schema/rabbit/spring-rabbit.xsd
       http://www.springframework.org/schema/task
       http://www.springframework.org/schema/task/spring-task.xsd">

    <!--启用注解监听消息-->
    <rabbit:annotation-driven/>

    <!--连接工厂配置-->
    <rabbit:connection-factory id="rabbitConnectionFactory"
                               thread-factory="amqpThreadFactory"
                               virtual-host="${rabbitmq.virtual-host:/}"
                               username="${rabbitmq.username}"
                               password="${rabbitmq.password}"
                               channel-cache-size="${rabbitmq.channel-cache-size:30}"
                               addresses="${rabbitmq.addresses}"/>

    <bean id="amqpThreadFactory" class="org.springframework.scheduling.concurrent.CustomizableThreadFactory">
        <constructor-arg value="rabbitmq-"/>
    </bean>

    <!--消息模板-->
    <rabbit:template id="amqpTemplate" connection-factory="rabbitConnectionFactory"
                     message-converter="amqpMessageConverter"/>

    <!--消息转换，生产者和消费者都需要 -->
    <bean id="amqpMessageConverter" class="org.springframework.amqp.support.converter.Jackson2JsonMessageConverter"/>

    <!--amqp管理-->
    <rabbit:admin id="amqpAdmin" connection-factory="rabbitConnectionFactory"/>

    <!--消息监听容器，配合注解监听消息-->
    <bean id="rabbitListenerContainerFactory"
          class="org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory">
        <property name="connectionFactory" ref="rabbitConnectionFactory"/>
        <!--并发消费者数量-->
        <property name="concurrentConsumers" value="${rabbitmq.concurrentConsumers:3}"/>
        <!--最大数量-->
        <property name="maxConcurrentConsumers" value="${rabbitmq.maxConcurrentConsumers:10}"/>
        <!--消息转换-->
        <property name="messageConverter" ref="amqpMessageConverter"/>
        <!--任务线程池-->
        <property name="taskExecutor">
            <task:executor id="amqpTaskExecutor" pool-size="${rabbitmq.task-executor.pool-size:100}"/>
        </property>
        <!--手动确认-->
        <property name="acknowledgeMode" value="${rabbitmq.acknowledgeMode:MANUAL}"/>
    </bean>

</beans>
```

3 [Spring 配置文件](https://github.com/abel533/spring-rabbitmq-demo/blob/master/src/test/resources/META-INF/spring/application.properties)中需要提供的配置

```properties
# rabbitmq 消息配置
rabbitmq.addresses=localhost:5672
rabbitmq.virtual-host=/
rabbitmq.username=root
rabbitmq.password=root
rabbitmq.channel-cache-size=50
rabbitmq.concurrentConsumers=3
rabbitmq.maxConcurrentConsumers=10
# 确认方式 MANUAL 手动，AUTO 自动，NONE 自动确认
rabbitmq.acknowledgeMode=MANUAL
# 线程池数量 = 并发数 * 监听数
rabbitmq.task-executor.pool-size=100
```

下面是和 Spring 集成后的用法。

测试中，增加了 [spring.xml](https://github.com/abel533/spring-rabbitmq-demo/blob/master/src/test/resources/META-INF/spring/spring.xml) 配置文件，内容如下：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns="http://www.springframework.org/schema/beans"
       xmlns:task="http://www.springframework.org/schema/task"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/task
       http://www.springframework.org/schema/task/spring-task.xsd
       http://www.springframework.org/schema/context
       http://www.springframework.org/schema/context/spring-context.xsd">

    <!--加载属性配置文件-->
    <context:property-placeholder location="classpath:META-INF/spring/application.properties"/>

    <!--扫描包-->
    <context:component-scan base-package="rabbitmq"/>

    <!--Producter 中的任务调度使用-->
    <task:scheduler id="taskScheduler"/>
    <task:annotation-driven scheduler="taskScheduler"/>

    <!--引入 spring-rabbitmq 配置-->
    <import resource="classpath*:META-INF/spring/spring-rabbitmq.xml"/>

</beans>
```

### 生产者
[示例代码](https://github.com/abel533/spring-rabbitmq-demo/blob/master/src/test/java/rabbitmq/Producter.java) 如下：
```java
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
```
1. 在代码中直接注入 `AmqpTemplate`，用于发送或接收消息。
2. 根据需要注入 `AmqpAdmin`，可以用于创建交换机、队列和绑定。

上面代码中，在 `init` 初始化中定义了一个交换机。通过 `product` 定时任务，每隔 1000 毫秒执行一次，调用 `template.convertAndSend("logger", key, obj);` 发送消息，发送的对象会根据前面 spring-rabbit 配置文件中的消息转换器转换为 JSON 数据进行发送。

生产者的逻辑可以根据业务需要进行定制。

### 消费者

消费者有多种用法，这里使用最方便的注解用法。

在 [Consumer](https://github.com/abel533/spring-rabbitmq-demo/blob/master/src/test/java/rabbitmq/listener/Consumer.java) 代码中，有 3 个例子，这里拿第一个进行讲解：

```java
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
        exchange = @Exchange(value = "logger",
                             durable = "true",
                             ignoreDeclarationExceptions = "true",
                             type = ExchangeTypes.TOPIC),
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
```
#### 注解

消费者监听的主要注解就是 `@RabbitListener`，上面例子是一个比较复杂的用法，下面从简单开始说起。
最简单的情况下，注解用法如下：
```java
@RabbitListener(queues = "myQueue")
public void processOrder(String data) {
    ...
}
```
这种情况下，要求 `myQueue` 队列已经存在，这样就能直接监听该队列。除此之外这里接收的参数要求是字符串类型，和消费者发送的消息类型需要一致。

再稍微简单点的情况下，用法如下：
```java
@RabbitListener(bindings = @QueueBinding(
      value = @Queue,
      exchange = @Exchange(value = "auto.exch"),
      key = "invoiceRoutingKey")
)
public void processInvoice(String data) {
  ...
}
```
实际上这里已经有些复杂了，这个例子的特点就是，不需要事先存在交换机、队列和绑定。Spring 在启动的时候会根据这里的注解去创建这三者（RabbitMQ 规则是如果队列、交换机已经存在，在参数相同的情况下会直接复用，不会创建新的，如果参数不同会报错）。这里的队列只用了 `@Queue`，因此会创建一个匿名独占自动删除的队列。交换机的名字指定了 `auto.exch`，队列和交换机通过 `invoiceRoutingKey` 进行绑定。

现在再来看本例的用法：
```java
@RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "logger.all", durable = "true"),
        exchange = @Exchange(value = "logger",
                             durable = "true",
                             ignoreDeclarationExceptions = "true",
                             type = ExchangeTypes.TOPIC),
        key = "logger.#"
))
```
这里创建了一个指定名称的队列，并且配置了持久化。还创建了一个支持持久化的交换机，类型为 `TOPIC`，并且忽略交换机的声明异常（如果已经存在并且属性不同时，忽略此异常）。通过 `logger.#` 进行匹配，在主题交换机中，有两个特殊的字符 `*` 和 `#`，分别匹配一个逗号隔开的单词和任意（可0）单词。因此这里能匹配 `logger.info`， `logger.xxx.debug` 等路由。

除了上面这些常见用法外，还有一个特殊的情况，可以根据接收类型自动匹配的用法，如下：
```java
@RabbitListener(id="multi", queues = "someQueue")
public class MultiListenerBean {

    @RabbitHandler
    @SendTo("my.reply.queue")
    public String bar(Bar bar) {
        ...
    }

    @RabbitHandler
    public String baz(Baz baz) {
        ...
    }

    @RabbitHandler
    public String qux(@Header("amqp_receivedRoutingKey") String rk, @Payload Qux qux) {
        ...
    }

}
```

在类上使用了 `@RabbitListener` 注解，在方法上使用了 `@RabbitHandler` 注解。在监听 `someQueue` 队列时，会根据消息的实际类型，调用匹配的方法（`Bar`, `Baz` 和 `Qux`）。

>特别注意：只有上面这种用法下才会根据类型进行匹配，直接在方法上使用 `@RabbitListener` 注解时不会自动匹配。

下面来看看这个参数需要注意的地方。

#### 参数
在我们配置的 JSON 转换中，除了转换的 JSON 串之外，在消息中还记录了类型的信息。如下图所示：
![在这里插入图片描述](https://img-blog.csdnimg.cn/20181202102219821.png)
可以看到在消息属性头中，通过 `__TypeId__` 记录了消息对象的实际类型，因此在 Spring 中的序列化和反序列化中能够根据这里的类型进行转换，当接收类型和这里指定的类型不一致时会报错（只有前面 `@RabbitHandler` 用法中会去匹配正确的方法，无法匹配时报错）。

Spring AMQP 中支持以下几类参数：

1. 消息对象(payload)，如果参数类型不能明确匹配时，需要通过 `@Payload` 指定消息体。
2. `com.rabbitmq.client.Channel`，消息通道，可以调用 AMQP 的基本方法，常用于 ack 和 reject。
3. `@Header` 注解的参数，从消息头提取指定的信息。
4. `org.springframework.amqp.core.Message` 消息的原始对象。
5. `org.springframework.messaging.Message<T>` 消息接口，通过泛型指定消息体类型，可以在 1 的基础上额外获取消息头信息。

#### ack 和 reject

在本例中，由于要手动 ACK 或 REJECT，所以在消息体之外还注入了 `@Header(AmqpHeaders.DELIVERY_TAG) Long deliveryTag` 和 `Channel`。

在业务逻辑执行完成后或者发生异常时，根据具体的情况来选择执行。

如果业务顺利执行完成，我们可以直接通过 `channel.basicAck(deliveryTag, false);` 确认消费，此后消息队列会删除这条已消费的消息。

如果业务中出现了异常，需要具体分析，如果只是网络或可以重试的问题，我们可以通过 `channel.basicReject(deliveryTag, true);` 将消息返还给消息队列。如果出现的是问题是业务逻辑或者就算重复执行仍然有问题的情况，可能就需要通过 `channel.basicReject(deliveryTag, false);`删除该消息（存在死信队列的情况会接收该消息，可以进行后续处理）。

## 总结

学会使用 RabbitMQ 是一件很容易的事情，但是用好用对是很不容易的事。不同常见和业务都需要考虑使用什么类型的交换机，使用什么样的队列，每个队列分配多少个并发，这些都很重要。

想要真正用好消息队列，还需要学习很多知识，你可以通过下面的参考资料了解更多。

## 参考资料

在我学 RabbitMQ 的过程中，下面这些资料是特别有用的，都是官方提供的项目文档，必要的时候可以多看几遍。

1. https://www.rabbitmq.com
2. https://www.rabbitmq.com/man/rabbitmqctl.8.html
3. https://docs.spring.io/spring-amqp/docs/1.7.11.RELEASE/reference/html/index.html

除此之外，我还参考了下面两本书：

- RabbitMQ实战：高效部署分布式消息队列
- RabbitMQ实战指南

第一本书更多的像文档，第二本书有更多作者的心得和技巧。
