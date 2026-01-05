package rs.ac.ftn.isa.isabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class IsabackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(IsabackendApplication.class, args);
	}

}
