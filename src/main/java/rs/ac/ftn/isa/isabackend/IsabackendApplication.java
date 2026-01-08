package rs.ac.ftn.isa.isabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class IsabackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(IsabackendApplication.class, args);
	}

}
