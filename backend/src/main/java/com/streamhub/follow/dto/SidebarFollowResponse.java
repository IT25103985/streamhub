package com.streamhub.follow.dto;

import java.util.List;

//response payload for sidebar containing total following count and top creators
public class SidebarFollowResponse {
    private long totalFollowing;
    private List<SidebarFollowedUserDTO> topFollowed;

    public SidebarFollowResponse() {}

    public SidebarFollowResponse(long totalFollowing, List<SidebarFollowedUserDTO> topFollowed) {
        this.totalFollowing = totalFollowing;
        this.topFollowed = topFollowed;
    }

    public long getTotalFollowing() {
        return totalFollowing;
    }

    public void setTotalFollowing(long totalFollowing) {
        this.totalFollowing = totalFollowing;
    }

    public List<SidebarFollowedUserDTO> getTopFollowed() {
        return topFollowed;
    }

    public void setTopFollowed(List<SidebarFollowedUserDTO> topFollowed) {
        this.topFollowed = topFollowed;
    }
}
