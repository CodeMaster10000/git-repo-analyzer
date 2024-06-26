package com.codemaster.git_repo_analyzer.persistence.repository;

import com.codemaster.git_repo_analyzer.persistence.entity.ApplicationJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface ApplicationJobRepository extends JpaRepository<ApplicationJobEntity, Integer> {

}
