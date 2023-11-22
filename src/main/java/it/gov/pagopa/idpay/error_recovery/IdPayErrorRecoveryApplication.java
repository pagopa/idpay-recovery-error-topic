package it.gov.pagopa.idpay.error_recovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
public class IdPayErrorRecoveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdPayErrorRecoveryApplication.class, args);
    }
}