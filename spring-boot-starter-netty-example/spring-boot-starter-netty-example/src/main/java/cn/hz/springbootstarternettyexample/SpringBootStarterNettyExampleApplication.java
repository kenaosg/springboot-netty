package cn.hz.springbootstarternettyexample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.ConfigurableApplicationContext;

@EnableEurekaClient
@SpringBootApplication(scanBasePackages = {"cn.hz"})
public class SpringBootStarterNettyExampleApplication {

	public static ConfigurableApplicationContext applicationContext;

	public static void main(String[] args) {
		applicationContext =
				SpringApplication.run(SpringBootStarterNettyExampleApplication.class, args);
	}
}
