# springboot-netty

## Introduction
The repository contains two parts：

### spring-boot-starter-netty
A netty based embedded servlet container.

### spring-boot-starter-netty-example
An example, which shows how to use it, and how to use it with SpringCloud components, like Eureka, Ribbon.
Much thanks to @DanielThomas and his project https://github.com/DanielThomas/spring-boot-starter-netty.

## Compile and Use
### How to compile and install it into local maven repository

Just run build script under [springboot-netty/spring-boot-starter-netty/build](https://github.com/gangsun/springboot-netty/tree/master/spring-boot-starter-netty/build). It will compile the project to jar file and install the jar into local maven repository

### How to use it

Add below dependencies to your pom file. Note that I haven't uploaded it to maven central repository, so you should install it into your local maven repository before adding it to your pom.
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
  <exclusions>
    <exclusion>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-tomcat</artifactId>
    </exclusion>
  </exclusions>
</dependency>

<dependency>
  <groupId>cn.hz</groupId>
  <artifactId>spring-boot-starter-netty</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Run
```java
@EnableEurekaClient
@SpringBootApplication(scanBasePackages = {"cn.hz"})
public class SpringBootStarterNettyExampleApplication {

	public static ConfigurableApplicationContext applicationContext;

	public static void main(String[] args) {
		applicationContext = SpringApplication.run(SpringBootStarterNettyExampleApplication.class, args);
	}
}
```
