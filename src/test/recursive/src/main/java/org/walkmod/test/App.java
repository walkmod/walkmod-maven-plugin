package org.walkmod.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.ArrayList;

@SpringBootApplication
public class App {

    private boolean used;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    private void unused() {}

}