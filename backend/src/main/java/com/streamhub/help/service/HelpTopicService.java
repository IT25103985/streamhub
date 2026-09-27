package com.streamhub.help.service;

import com.streamhub.common.exception.ResourceNotFoundException;
import com.streamhub.help.dto.HelpTopicRequest;
import com.streamhub.help.dto.HelpTopicResponse;
import com.streamhub.help.entity.HelpTopic;
import com.streamhub.help.repository.HelpTopicRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
//service managing creation editing deletion and ordering of help topics
public class HelpTopicService {

    private final HelpTopicRepository helpTopicRepository;

    public HelpTopicService(HelpTopicRepository helpTopicRepository) {
        this.helpTopicRepository = helpTopicRepository;
    }

    //retrieves all help topics sorted by section and display order
    @Transactional(readOnly = true)
    public List<HelpTopicResponse> listAll() {
        return helpTopicRepository.findAllByOrderBySectionAscDisplayOrderAsc().stream()
                .map(HelpTopicResponse::fromEntity)
                .collect(Collectors.toList());
    }

    //creates and persists a new help topic entry
    @Transactional
    public HelpTopicResponse create(HelpTopicRequest request) {
        HelpTopic topic = new HelpTopic(
                request.getSection().trim(),
                request.getQuestion().trim(),
                request.getAnswer().trim(),
                request.getDisplayOrder() != null ? request.getDisplayOrder() : 0
        );
        HelpTopic saved = helpTopicRepository.save(topic);
        return HelpTopicResponse.fromEntity(saved);
    }

    //updates existing help topic attributes
    @Transactional
    public HelpTopicResponse update(UUID id, HelpTopicRequest request) {
        HelpTopic topic = helpTopicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Help topic not found"));

        topic.setSection(request.getSection().trim());
        topic.setQuestion(request.getQuestion().trim());
        topic.setAnswer(request.getAnswer().trim());
        topic.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);

        HelpTopic saved = helpTopicRepository.save(topic);
        return HelpTopicResponse.fromEntity(saved);
    }

    //removes a help topic by identifier
    @Transactional
    public void delete(UUID id) {
        HelpTopic topic = helpTopicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Help topic not found"));

        helpTopicRepository.delete(topic);
    }

    //initializes default help topics if table is empty
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedInitialHelpTopics() {
        try {
            if (helpTopicRepository.count() > 0) {
                return;
            }

            List<HelpTopic> defaultTopics = Arrays.asList(
                // Getting Started / Account
                new HelpTopic(
                    "Getting Started / Account",
                    "How do I create a new StreamHub account?",
                    "Click the Log In button in the top navigation bar and select Register. Enter your email, username, and password to set up your account and choose your preferred category interests.",
                    1
                ),
                new HelpTopic(
                    "Getting Started / Account",
                    "How do I update my profile details and avatar?",
                    "Open the profile dropdown in the top header and choose View Profile. From there you can edit your profile information and upload a custom avatar.",
                    2
                ),
                new HelpTopic(
                    "Getting Started / Account",
                    "How do I log out of my account?",
                    "Open the profile menu in the upper-right corner and select Log out. You can also sign out from the bottom of the home sidebar.",
                    3
                ),

                // Uploading & Managing Videos
                new HelpTopic(
                    "Uploading & Managing Videos",
                    "How do I upload a video to StreamHub?",
                    "Click the Upload button in the top header. Select your video file and thumbnail, provide a title, description, and category, and click Upload Video.",
                    1
                ),
                new HelpTopic(
                    "Uploading & Managing Videos",
                    "What video formats and limits are supported?",
                    "StreamHub supports standard MP4, WebM, and MKV video files up to 500MB. Thumbnails should be in JPG, PNG, or WebP format under 2MB.",
                    2
                ),
                new HelpTopic(
                    "Uploading & Managing Videos",
                    "How can I edit or delete a video I uploaded?",
                    "Navigate to My Uploads from the profile dropdown menu. Use the options menu on any of your videos to update details or delete the video.",
                    3
                ),

                // Categories
                new HelpTopic(
                    "Categories",
                    "How do categories work on StreamHub?",
                    "Categories organize videos by topics such as Gaming, Music, Education, and Tech. You can browse videos under any category using the chips on the homepage.",
                    1
                ),
                new HelpTopic(
                    "Categories",
                    "Can I filter search results by category?",
                    "Yes, when searching or browsing the library, clicking any category chip filters the results to only include videos within that genre.",
                    2
                ),

                // Playlists
                new HelpTopic(
                    "Playlists",
                    "How do I create and manage a playlist?",
                    "Visit My Playlists from the profile dropdown and click Create Playlist. You can customize the name, description, privacy, and cover image.",
                    1
                ),
                new HelpTopic(
                    "Playlists",
                    "How do I add videos to my playlist?",
                    "While watching any video, click the Add to Playlist button beneath the player to include it in one of your playlists.",
                    2
                ),
                new HelpTopic(
                    "Playlists",
                    "Who can view my playlists?",
                    "Public playlists are visible to the community on the homepage and public profile pages. Private playlists are visible only to you.",
                    3
                ),

                // Comments & Likes
                new HelpTopic(
                    "Comments & Likes",
                    "How do I like or comment on a video?",
                    "Use the thumbs-up button below any video to like it, or type your thoughts in the comment field under the player and click Comment.",
                    1
                ),
                new HelpTopic(
                    "Comments & Likes",
                    "Can I reply to other comments?",
                    "Yes, click the Reply button under any comment to reply in a threaded conversation. The original commenter will be notified.",
                    2
                ),
                new HelpTopic(
                    "Comments & Likes",
                    "Can I edit or delete my comments?",
                    "You can edit or remove any comment you posted using the action menu next to your comment text.",
                    3
                ),

                // Reporting Content
                new HelpTopic(
                    "Reporting Content",
                    "When should I report content?",
                    "If you notice content containing harassment, copyright violations, or harmful behavior, submit a report for moderation review.",
                    1
                ),
                new HelpTopic(
                    "Reporting Content",
                    "How do I report a video, comment, or playlist?",
                    "Click the Report button adjacent to the item, pick a reason from the list, describe the issue, and submit.",
                    2
                ),
                new HelpTopic(
                    "Reporting Content",
                    "Where can I see the status of my reports?",
                    "Open My Reports from your profile menu to see your submitted reports and outcomes once reviewed by moderators.",
                    3
                ),

                // Watch History & Recommendations
                new HelpTopic(
                    "Watch History & Recommendations",
                    "How does Watch History work?",
                    "StreamHub keeps track of videos you view so you can resume where you left off from the Watch History page.",
                    1
                ),
                new HelpTopic(
                    "Watch History & Recommendations",
                    "How are video recommendations determined?",
                    "Recommendations are suggested based on your preferred categories and the videos you watch regularly.",
                    2
                ),
                new HelpTopic(
                    "Watch History & Recommendations",
                    "Can I clear my watch history?",
                    "Yes, visit the Watch History page and click Clear History to reset your viewing log or delete individual videos.",
                    3
                )
            );

            helpTopicRepository.saveAll(defaultTopics);
        } catch (Exception ex) {
            System.err.println("Could not seed initial help topics: " + ex.getMessage());
        }
    }
}
