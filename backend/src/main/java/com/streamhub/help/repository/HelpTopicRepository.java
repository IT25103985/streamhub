package com.streamhub.help.repository;

import com.streamhub.help.entity.HelpTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
//repository providing database queries for help topic entities
public interface HelpTopicRepository extends JpaRepository<HelpTopic, UUID> {

    List<HelpTopic> findAllByOrderBySectionAscDisplayOrderAsc();
}
