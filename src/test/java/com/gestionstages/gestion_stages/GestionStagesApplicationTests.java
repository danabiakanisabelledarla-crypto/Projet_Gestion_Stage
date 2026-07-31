package com.gestionstages.gestion_stages;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:contexttest;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class GestionStagesApplicationTests {

	@Test
	void contextLoads() {
	}

}
