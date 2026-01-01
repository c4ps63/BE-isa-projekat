package rs.ac.ftn.isa.isabackend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import rs.ac.ftn.isa.isabackend.model.Comment;
import rs.ac.ftn.isa.isabackend.model.Role;
import rs.ac.ftn.isa.isabackend.model.User;
import rs.ac.ftn.isa.isabackend.model.Video;
import rs.ac.ftn.isa.isabackend.service.CommentService;
import rs.ac.ftn.isa.isabackend.service.UserService;
import rs.ac.ftn.isa.isabackend.service.VideoService;

@Component
public class TestDataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final VideoService videoService;
    private final CommentService commentService;

    @Autowired
    public TestDataInitializer(UserService userService, VideoService videoService, CommentService commentService) {
        this.userService = userService;
        this.videoService = videoService;
        this.commentService = commentService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Proveri da li već postoje podaci
        if (userService.existsByUsername("marko")) {
            return; // Već su dodati test podaci
        }

        // Kreiraj test korisnike
        User user1 = new User();
        user1.setUsername("marko");
        user1.setEmail("marko@example.com");
        user1.setPassword("password123"); // TODO: Hash-ovati lozinku kasnije
        user1.setRole(Role.USER);
        user1.setBio("Volim da pravim video sadržaj!");
        user1.setAvatarUrl("https://picsum.photos/id/53/600/500");
        userService.save(user1);

        User user2 = new User();
        user2.setUsername("ana");
        user2.setEmail("ana@example.com");
        user2.setPassword("password123");
        user2.setRole(Role.USER);
        user2.setBio("Tech enthusiast");
        user1.setAvatarUrl("https://picsum.photos/id/41/600/500");
        userService.save(user2);

        // Kreiraj test videe
        Video video1 = new Video();
        video1.setTitle("Uvod u Spring Boot");
        video1.setDescription("Naučite osnove Spring Boot frameworka");
        video1.setVideoUrl("https://test-videos.co.uk/vids/sintel/mp4/h264/720/Sintel_720_10s_1MB.mp4");
        video1.setThumbnailUrl("https://picsum.photos/id/31/1000/500");
        video1.setOwner(user1);
        videoService.save(video1);

        Video video2 = new Video();
        video2.setTitle("Angular za početnike");
        video2.setDescription("Kompletna Angular aplikacija od nule");
        video2.setVideoUrl("https://test-videos.co.uk/vids/sintel/mp4/h264/720/Sintel_720_10s_1MB.mp4");
        video2.setThumbnailUrl("https://picsum.photos/id/27/1000/500");
        video2.setOwner(user1);
        videoService.save(video2);

        Video video3 = new Video();
        video3.setTitle("PostgreSQL baze podataka");
        video3.setDescription("Kako efikasno koristiti PostgreSQL");
        video3.setVideoUrl("https://test-videos.co.uk/vids/sintel/mp4/h264/720/Sintel_720_10s_1MB.mp4");
        video3.setThumbnailUrl("https://picsum.photos/id/59/1000/500");
        video3.setOwner(user2);
        videoService.save(video3);

        // Kreiraj test komentare
        Comment comment1 = new Comment();
        comment1.setText("Odličan tutorial! Mnogo mi je pomogao.");
        comment1.setAuthor(user2);
        comment1.setVideo(video1);
        commentService.save(comment1);

        Comment comment2 = new Comment();
        comment2.setText("Hvala na detaljnom objašnjenju!");
        comment2.setAuthor(user1);
        comment2.setVideo(video3);
        commentService.save(comment2);

        System.out.println("✅ Test podaci uspešno dodati u bazu!");
    }
}