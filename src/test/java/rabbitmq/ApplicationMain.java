package rabbitmq;

import org.springframework.context.support.GenericXmlApplicationContext;

public class ApplicationMain {

    public static void main(String[] args) throws InterruptedException {
        GenericXmlApplicationContext context =
            new GenericXmlApplicationContext("classpath:/META-INF/spring/spring.xml");
        context.start();
    }

}
