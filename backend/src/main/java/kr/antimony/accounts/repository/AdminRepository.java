package kr.antimony.accounts.repository;

import kr.antimony.accounts.domain.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, Long> {
}
