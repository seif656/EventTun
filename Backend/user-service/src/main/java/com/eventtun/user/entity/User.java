
    package com.eventtun.user.entity;

    import jakarta.persistence.*;

    @Entity
    @Table(name = "users")
    public class User {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false)
        private String firstName;

        @Column(nullable = false)
        private String lastName;

        @Column(nullable = false, unique = true)
        private String email;

        private String keycloakSubject;


        @Enumerated(EnumType.STRING)
        private Role role;

        @Enumerated(EnumType.STRING)
        private UserStatus status;


        public User() {
        }

        public void setId(Long id) {
            this.id = id;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public void setKeycloakSubject(String keycloakSubject) {
            this.keycloakSubject = keycloakSubject;
        }

        public Long getId() {
            return id;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getEmail() {
            return email;
        }

        public String getKeycloakSubject() {
            return keycloakSubject;
        }


        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }

        public UserStatus getStatus() {
            return status;
        }

        public void setStatus(UserStatus status) {
            this.status = status;
        }

    }
