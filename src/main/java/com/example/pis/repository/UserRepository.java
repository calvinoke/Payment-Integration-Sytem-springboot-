package com.example.pis.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.pis.entity.User;

/**
 * Repository for User entities.
 * Provides CRUD operations and custom finder methods.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their unique username.
     * 
     * @param username the username
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);
}
