package rs.ac.ftn.isa.isabackend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import rs.ac.ftn.isa.isabackend.model.Comment;
import rs.ac.ftn.isa.isabackend.model.Role;
import rs.ac.ftn.isa.isabackend.model.User;
import rs.ac.ftn.isa.isabackend.model.Video;
import rs.ac.ftn.isa.isabackend.repository.RoleRepository;
import rs.ac.ftn.isa.isabackend.repository.UserRepository;
import rs.ac.ftn.isa.isabackend.repository.VideoRepository;
import rs.ac.ftn.isa.isabackend.service.CommentService;
import rs.ac.ftn.isa.isabackend.service.VideoService;

import java.util.List;
import java.util.Optional;

@Component
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final VideoService videoService;
    private final VideoRepository videoRepository;
    private final CommentService commentService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public TestDataInitializer(UserRepository userRepository, RoleRepository roleRepository, VideoService videoService, VideoRepository videoRepository, CommentService commentService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.videoService = videoService;
        this.videoRepository = videoRepository;
        this.commentService = commentService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. KREIRANJE ROLA
        if (roleRepository.count() == 0) {
            Role userRole = new Role();
            userRole.setName("ROLE_USER");
            roleRepository.save(userRole);

            Role adminRole = new Role();
            adminRole.setName("ROLE_ADMIN");
            roleRepository.save(adminRole);
        }

        List<Role> roles = roleRepository.findByName("ROLE_USER");

        // 2. KREIRANJE KORISNIKA

        User user1;
        Optional<User> user1Opt = userRepository.findByUsername("marko");
        if (user1Opt.isEmpty()) {
            user1 = new User();
            user1.setUsername("marko");
            user1.setEmail("marko@example.com");
            user1.setPassword(passwordEncoder.encode("password123"));
            user1.setFirstName("Marko");
            user1.setLastName("Markovic");
            user1.setAddress("Bulevar Oslobodjenja 1");
            user1.setRoles(roles);
            user1.setBio("Volim da pravim video sadržaj!");
            user1.setAvatarUrl("https://picsum.photos/id/53/600/500");
            user1.setEnabled(true);
            user1 = userRepository.save(user1);
        } else {
            user1 = user1Opt.get();
        }

        User user2;
        Optional<User> user2Opt = userRepository.findByUsername("ana");
        if (user2Opt.isEmpty()) {
            user2 = new User();
            user2.setUsername("ana");
            user2.setEmail("ana@example.com");
            user2.setPassword(passwordEncoder.encode("password123"));
            user2.setFirstName("Ana");
            user2.setLastName("Anic");
            user2.setAddress("Bulevar Oslobodjenja 2");
            user2.setRoles(roles);
            user2.setBio("Tech enthusiast");
            user2.setAvatarUrl("https://picsum.photos/id/41/600/500");
            user2.setEnabled(true);
            user2 = userRepository.save(user2);
        } else {
            user2 = user2Opt.get();
        }

        // 3. KREIRANJE VIDEA
        if (videoRepository.count() == 0) {
            Video video1 = new Video();
            video1.setTitle("Video1");
            video1.setDescription("Opis videa 1");
            video1.setVideoUrl("video1.mp4");
            video1.setThumbnailUrl("image_3.jpg");
            video1.setDuration(11);
            video1.setViewCount(150L);
            video1.setOwner(user1);
            videoService.save(video1);

            Video video2 = new Video();
            video2.setTitle("Alen i Indira");
            video2.setDescription("Muzikaaa");
            video2.setVideoUrl("alen.mp4");
            video2.setThumbnailUrl("alen.jpg");
            video2.setDuration(200);
            video2.setViewCount(1250L);
            video2.setOwner(user1);
            videoService.save(video2);

            Video video3 = new Video();
            video3.setTitle("Video neki tamo");
            video3.setDescription("Ma nekakav opis hahah");
            video3.setVideoUrl("video3.mp4");
            video3.setThumbnailUrl("test.jpg");
            video3.setDuration(10);
            video3.setViewCount(85L);
            video3.setOwner(user2);
            videoService.save(video3);

            // Komentari
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

            System.out.println("--- Test podaci (Videi i Komentari) uspesno dodati! ---");
        }
    }
}