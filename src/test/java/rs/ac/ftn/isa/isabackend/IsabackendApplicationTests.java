package rs.ac.ftn.isa.isabackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import rs.ac.ftn.isa.isabackend.model.User;
import rs.ac.ftn.isa.isabackend.model.Video;
import rs.ac.ftn.isa.isabackend.repository.UserRepository;
import rs.ac.ftn.isa.isabackend.repository.VideoRepository;
import rs.ac.ftn.isa.isabackend.service.VideoService;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class IsabackendApplicationTests {

	@Autowired
	private VideoService videoService;

	@Autowired
	private VideoRepository videoRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void contextLoads() {
		// Ovaj test samo proverava da li se aplikacija podiže
	}

	@Test
	public void testConcurrentViewIncrements() throws InterruptedException {
		System.out.println("=== POČETAK TESTA KONKURENTNOSTI ===");

		// 1. PRIPREMA PODATAKA
		// Kreiramo privremenog korisnika
		User owner = new User();
		owner.setUsername("testuser_concurrency");
		owner.setEmail("test_concurrency@test.com");
		owner.setPassword("password");
		owner.setFirstName("Test");
		owner.setLastName("User");
		// Čuvamo ga direktno preko repozitorijuma da izbegnemo validacije servisa
		owner = userRepository.save(owner);

		// Kreiramo video sa 0 pregleda
		Video video = new Video();
		video.setTitle("Concurrency Test Video");
		video.setVideoUrl("test.mp4");
		video.setViewCount(0L);
		video.setOwner(owner);
		video.setUploadedAt(LocalDateTime.now());
		video = videoRepository.save(video);

		Long videoId = video.getId();

		// 2. SIMULACIJA (100 korisnika istovremeno)
		int numberOfThreads = 100;
		ExecutorService executorService = Executors.newFixedThreadPool(10);

		for (int i = 0; i < numberOfThreads; i++) {
			executorService.submit(() -> {
				try {
					// Svaka nit poziva metodu za povećanje pregleda
					videoService.incrementViewCount(videoId);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}

		// Čekamo da svi završe
		executorService.shutdown();
		boolean finished = executorService.awaitTermination(1, TimeUnit.MINUTES);

		// 3. PROVERA
		// Uzimamo svežu verziju videa iz baze
		Video updatedVideo = videoRepository.findById(videoId).orElseThrow();

		System.out.println("Očekivano pregleda: " + numberOfThreads);
		System.out.println("Stvarno pregleda u bazi: " + updatedVideo.getViewCount());

		// 4. BRISANJE PODATAKA (CLEANUP)
		videoRepository.deleteById(videoId);
		userRepository.deleteById(owner.getId());

		// Assert - Test pada ako brojevi nisu isti
		assertEquals(numberOfThreads, updatedVideo.getViewCount());

		System.out.println("=== TEST USPEŠNO ZAVRŠEN ===");
	}

}