package ue.edu.co.splitbill;

import org.springframework.boot.SpringApplication;

public class TestSplitBillApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(SplitBillApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
