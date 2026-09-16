import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import axiosInstance from '../api/axiosInstance';
import { playlistService } from '../features/playlist/playlistService';
import VideoCard from '../components/VideoCard';
import PlaylistCard from '../features/playlist/PlaylistCard';
import VideoGridSkeleton from '../components/skeletons/VideoGridSkeleton';
import VideoCardSkeleton from '../components/skeletons/VideoCardSkeleton';
import PlaylistCardSkeleton from '../components/skeletons/PlaylistCardSkeleton';
import CategoryChipBar from '../components/CategoryChipBar';
import SearchResultsTabs from '../features/search/SearchResultsTabs';
import VideoWatchPanel from '../features/video/VideoWatchPanel';
import HomeSidebar from '../components/HomeSidebar';
import watchHistoryService from '../features/watchhistory/watchHistoryService';
import './HomePage.css';
import '../features/search/SearchResults.css';

const PAGE_SIZE = 8;

//global search results page with tabbed videos and playlists views
const SearchResultsPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  const query = searchParams.get('q') || searchParams.get('search') || '';
  const categoryId = searchParams.get('categoryId');
  const activeTab = searchParams.get('tab') || 'videos';
  const page = parseInt(searchParams.get('page') || '0', 10);

  const [activeVideoId, setActiveVideoId] = useState(null);
  const [recommendedVideos, setRecommendedVideos] = useState([]);

  const [videos, setVideos] = useState([]);
  const [playlists, setPlaylists] = useState([]);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);

  //loads recommendations for the side navigation
  const fetchRecs = async () => {
    try {
      const recs = await watchHistoryService.getRecommendations(4);
      const filtered = (recs || []).filter((v) => {
        if (!v) return false;
        if ((v.viewCount || 0) < 10) return false;
        if (user) {
          const uId = user.userId || user.id;
          const vOwnerId = v.ownerId || v.uploaderId;
          if (uId && vOwnerId && String(uId).toLowerCase() === String(vOwnerId).toLowerCase()) {
            return false;
          }
          const uName = user.username;
          const vOwnerName = v.ownerUsername || v.creator || v.uploaderUsername;
          if (uName && vOwnerName && String(uName).toLowerCase() === String(vOwnerName).toLowerCase()) {
            return false;
          }
        }
        return true;
      });
      setRecommendedVideos(filtered);
    } catch (err) {
      console.error('Error fetching recommendations', err);
    }
  };

  //queries video results matching query and category filter
  const fetchVideos = async () => {
    setLoading(true);
    try {
      const params = { page: 0, size: PAGE_SIZE * (page + 1) };
      if (query) params.search = query;
      if (categoryId) params.categoryId = categoryId;

      const res = await axiosInstance.get('/videos', { params });
      const newVideos = res.data.content || res.data || [];
      const isLast = res.data.last !== undefined ? res.data.last : newVideos.length < (PAGE_SIZE * (page + 1));

      setVideos(newVideos);
      setHasMore(!isLast);
    } catch (err) {
      console.error('Error fetching videos search', err);
      setVideos([]);
      setHasMore(false);
    } finally {
      setLoading(false);
    }
  };

  //queries playlist results matching query and category filter
  const fetchPlaylists = async () => {
    setLoading(true);
    try {
      const params = { page: 0, size: PAGE_SIZE * (page + 1) };
      if (query) params.query = query;
      if (categoryId) params.categoryId = categoryId;

      const res = await playlistService.searchPlaylists(params);
      const newPlaylists = res.content || res || [];
      const isLast = res.last !== undefined ? res.last : newPlaylists.length < (PAGE_SIZE * (page + 1));

      setPlaylists(newPlaylists);
      setHasMore(!isLast);
    } catch (err) {
      console.error('Error fetching playlists search', err);
      setPlaylists([]);
      setHasMore(false);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRecs();
  }, []);

  //fetches active tab data on filter or search parameter change
  useEffect(() => {
    if (activeTab === 'playlists') {
      fetchPlaylists();
    } else {
      fetchVideos();
    }
  }, [query, categoryId, activeTab, page]);

  const handleCategoryChange = (catId) => {
    const newParams = new URLSearchParams(searchParams);
    if (catId) {
      newParams.set('categoryId', catId);
    } else {
      newParams.delete('categoryId');
    }
    newParams.set('page', '0');
    setSearchParams(newParams);
  };

  const handleTabChange = (newTab) => {
    const newParams = new URLSearchParams(searchParams);
    newParams.set('tab', newTab);
    newParams.set('page', '0');
    setSearchParams(newParams);
  };

  const handleLoadMore = () => {
    const newParams = new URLSearchParams(searchParams);
    newParams.set('page', (page + 1).toString());
    setSearchParams(newParams);
  };

  return (
    <div className="home-layout-container">
      <HomeSidebar
        recommendedVideos={recommendedVideos}
        onSelectVideo={(id) => setActiveVideoId(id)}
      />

      <main className="home-main-feed">
        <section className="block" id="library" style={{ paddingTop: '20px' }}>
          <div className="block-head" style={{ marginBottom: '14px' }}>
            <div>
              <h3>{query ? `Search results for "${query}"` : 'Browse library'}</h3>
            </div>
          </div>

          <CategoryChipBar
            selectedCategoryId={categoryId}
            onCategoryChange={handleCategoryChange}
          />

          <SearchResultsTabs
            activeTab={activeTab}
            onTabChange={handleTabChange}
          />

          <div className="block-inner">
            {activeTab === 'videos' ? (
              loading && videos.length === 0 ? (
                <VideoGridSkeleton count={8} id="videos-search-grid" />
              ) : (
                <div className="videos-grid home-4col-grid" id="videos-search-grid">
                  {videos.length > 0 ? (
                    videos.map((v) => (
                      <VideoCard
                        key={v.videoId || v.id}
                        video={v}
                        onClick={() => setActiveVideoId(v.videoId || v.id)}
                      />
                    ))
                  ) : (
                    !loading && <div className="empty-state">No videos found</div>
                  )}
                  {loading && videos.length > 0 && (
                    Array.from({ length: 4 }).map((_, i) => (
                      <VideoCardSkeleton key={`video-search-more-${i}`} />
                    ))
                  )}
                </div>
              )
            ) : (
              loading && playlists.length === 0 ? (
                <div className="videos-grid home-4col-grid" id="playlists-search-grid">
                  {Array.from({ length: 8 }).map((_, i) => (
                    <PlaylistCardSkeleton key={`playlist-search-skel-${i}`} />
                  ))}
                </div>
              ) : (
                <div className="videos-grid home-4col-grid" id="playlists-search-grid">
                  {playlists.length > 0 ? (
                    playlists.map((p) => (
                      <PlaylistCard
                        key={p.playlistId}
                        playlist={p}
                        isOwner={false}
                      />
                    ))
                  ) : (
                    !loading && <div className="empty-state">No playlists found</div>
                  )}
                  {loading && playlists.length > 0 && (
                    Array.from({ length: 4 }).map((_, i) => (
                      <PlaylistCardSkeleton key={`playlist-search-more-${i}`} />
                    ))
                  )}
                </div>
              )
            )}

            <div className="load-more-row">
              <button
                className="btn-load"
                id="load-more-btn"
                onClick={handleLoadMore}
                disabled={!hasMore || loading}
              >
                {hasMore ? (
                  <>
                    Load more {activeTab === 'videos' ? 'videos' : 'playlists'}{' '}
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M6 9l6 6 6-6" />
                    </svg>
                  </>
                ) : (
                  `You've reached the end of the ${activeTab === 'videos' ? 'videos' : 'playlists'}`
                )}
              </button>
            </div>
          </div>
        </section>
      </main>

      {activeVideoId && (
        <VideoWatchPanel
          videoId={activeVideoId}
          onClose={() => setActiveVideoId(null)}
        />
      )}
    </div>
  );
};

export default SearchResultsPage;
