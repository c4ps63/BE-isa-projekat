package rs.ac.ftn.isa.isabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.ftn.isa.isabackend.model.Role;
import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {
    List<Role> findByName(String name);
}