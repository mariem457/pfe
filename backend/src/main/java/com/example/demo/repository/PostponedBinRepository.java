package com.example.demo.repository;

import com.example.demo.entity.PostponedBin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostponedBinRepository extends JpaRepository<PostponedBin, Long> {

    List<PostponedBin> findByBinIdOrderByCreatedAtDesc(Long binId);

    List<PostponedBin> findByBinIdAndResolvedFalseOrderByCreatedAtDesc(Long binId);

    long countByBinId(Long binId);

    long countByBinIdAndResolvedFalse(Long binId);

    List<PostponedBin> findTop200ByOrderByCreatedAtDesc();

    List<PostponedBin> findByResolvedFalseOrderByCreatedAtDesc();

    List<PostponedBin> findByBinIdAndResolvedFalse(Long binId);
}