import { useState, useEffect, useContext } from 'react';
import { useSearchParams } from 'react-router-dom';
import axiosInstance from '../api/axiosInstance';
import { AuthContext } from '../context/AuthContext';
import watchHistoryService from '../features/watchhistory/watchHistoryService';
import { playlistService } from '../features/playlist/playlistService';
import VideoCard from '../components/VideoCard';
import PlaylistCard from '../features/playlist/PlaylistCard';
import VideoGridSkeleton from '../components/skeletons/VideoGridSkeleton';
import VideoCardSkeleton from '../components/skeletons/VideoCardSkeleton';
import PlaylistCardSkeleton from '../components/skeletons/PlaylistCardSkeleton';
import CategoryChipBar from '../components/CategoryChipBar';
import SearchResultsTabs from '../features/search/SearchResultsTabs';
import VideoWatchPanel from '../features/video/VideoWatchPanel';
import TrendingSection from '../features/video/TrendingSection';
import RecommendedSection from '../features/video/RecommendedSection';
import HomeSidebar from '../components/HomeSidebar';
import './HomePage.css';
import '../features/search/SearchResults.css';

const PAGE_SIZE = 8;

//main landing feed combining hero banner trending recommendations and tabbed library grid
const HomePage = () => {
  const { user } = useContext(AuthContext);
  const [searchParams, setSearchParams] = useSearchParams();
  
  const query = searchParams.get('q') || '';
  const categoryId = searchParams.get('categoryId');
  const activeTab = searchParams.get('tab') || 'videos';
  const page = parseInt(searchParams.get('page') || '0', 10);

  const [activeVideoId, setActiveVideoId] = useState(null);

  const [recommendedVideos, setRecommendedVideos] = useState([]);
  const [libraryVideos, setLibraryVideos] = useState([]);
  const [libraryPlaylists, setLibraryPlaylists] = useState([]);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  
  const fetchRecs = async () => {
    //only fetch recommendations for authenticated users
    if (!user) {
      setRecommendedVideos([]);
      return;
    }
    try {
      const recs = await watchHistoryService.getRecommendations(4);
      setRecommendedVideos(recs || []);
    } catch (err) {
      console.error('Error fetching recommendations', err);
      setRecommendedVideos([]);
    }
  };

  const fetchLibrary = async (isRefresh = false) => {
    if (!isRefresh) setLoading(true);
    try {
      const currentPage = isRefresh ? 0 : page;
      const params = { page: 0, size: PAGE_SIZE * (currentPage + 1) };
      if (query) params.search = query;
      if (categoryId) params.categoryId = categoryId;

      const res = await axiosInstance.get('/videos', { params });
      const newVideos = res.data.content || res.data || [];
      const isLast = res.data.last !== undefined ? res.data.last : newVideos.length < (PAGE_SIZE * (currentPage + 1));

      setLibraryVideos(newVideos);
      setHasMore(!isLast);
    } catch (err) {
      console.error('Error fetching library', err);
      setLibraryVideos([]);
      setHasMore(false);
    } finally {
      if (!isRefresh) setLoading(false);
    }
  };

  const fetchPlaylists = async (isRefresh = false) => {
    if (!isRefresh) setLoading(true);
    try {
      const currentPage = isRefresh ? 0 : page;
      const params = { page: 0, size: PAGE_SIZE * (currentPage + 1) };
      if (query) params.query = query;
      if (categoryId) params.categoryId = categoryId;

      const res = await playlistService.searchPlaylists(params);
      const newPlaylists = res.content || res || [];
      const isLast = res.last !== undefined ? res.last : newPlaylists.length < (PAGE_SIZE * (currentPage + 1));

      setLibraryPlaylists(newPlaylists);
      setHasMore(!isLast);
    } catch (err) {
      console.error('Error fetching library playlists', err);
      setLibraryPlaylists([]);
      setHasMore(false);
    } finally {
      if (!isRefresh) setLoading(false);
    }
  };

  //refetch feed whenever search category or tab filter updates
  useEffect(() => {
    fetchRecs();
    if (activeTab === 'playlists') {
      fetchPlaylists();
    } else {
      fetchLibrary();
    }
  }, [query, categoryId, activeTab, page, user]);

  //listen for cross tab updates from new uploads
  useEffect(() => {
    const handleRealtimeUpdate = () => {
      fetchRecs();
      if (activeTab === 'playlists') {
        fetchPlaylists(true);
      } else {
        fetchLibrary(true);
      }
    };

    window.addEventListener('streamhub-video-update', handleRealtimeUpdate);
    window.addEventListener('focus', handleRealtimeUpdate);

    //periodic background refresh for view count badges
    const interval = setInterval(handleRealtimeUpdate, 30000);

    //cleanup realtime update event listeners
    return () => {
      window.removeEventListener('streamhub-video-update', handleRealtimeUpdate);
      window.removeEventListener('focus', handleRealtimeUpdate);
      clearInterval(interval);
    };
  }, [query, categoryId, activeTab, page, user]);

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
        
        <section className="hero">
          <div className="hero-inner">
            <h2>Pure entertainment<br /><em>at your fingertips.</em></h2>
          </div>
        </section>

        {!query && !categoryId && activeTab === 'videos' && page === 0 && (
          <>
            <TrendingSection onSelectVideo={(id) => setActiveVideoId(id)} />
            {user && <RecommendedSection onSelectVideo={(id) => setActiveVideoId(id)} />}
          </>
        )}

        <section className="block" id="library" style={{ paddingTop: 0 }}>
          <div className="block-head" style={{ marginBottom: '14px' }}>
            <div>
              <h3>{query ? `Search results for "${query}"` : 'Browse by category'}</h3>
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
              loading && libraryVideos.length === 0 ? (
                <VideoGridSkeleton count={8} id="library-grid" />
              ) : (
                <div className="videos-grid home-4col-grid" id="library-grid">
                  {libraryVideos.length ? (
                    libraryVideos.map(v => (
                      <VideoCard 
                        key={v.videoId || v.id} 
                        video={v} 
                        onClick={() => setActiveVideoId(v.videoId || v.id)} 
                      />
                    ))
                  ) : (
                    !loading && <div className="empty-state">No videos found</div>
                  )}
                  {loading && libraryVideos.length > 0 && (
                    Array.from({ length: 4 }).map((_, i) => (
                      <VideoCardSkeleton key={`video-more-skel-${i}`} />
                    ))
                  )}
                </div>
              )
            ) : (
              loading && libraryPlaylists.length === 0 ? (
                <div className="videos-grid home-4col-grid" id="library-playlists-grid">
                  {Array.from({ length: 8 }).map((_, i) => (
                    <PlaylistCardSkeleton key={`playlist-skel-${i}`} />
                  ))}
                </div>
              ) : (
                <div className="videos-grid home-4col-grid" id="library-playlists-grid">
                  {libraryPlaylists.length ? (
                    libraryPlaylists.map(p => (
                      <PlaylistCard 
                        key={p.playlistId} 
                        playlist={p} 
                        isOwner={false}
                      />
                    ))
                  ) : (
                    !loading && <div className="empty-state">No playlists found</div>
                  )}
                  {loading && libraryPlaylists.length > 0 && (
                    Array.from({ length: 4 }).map((_, i) => (
                      <PlaylistCardSkeleton key={`playlist-more-skel-${i}`} />
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
                  <>Load more {activeTab === 'videos' ? 'videos' : 'playlists'} <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M6 9l6 6 6-6" /></svg></>
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

export default HomePage;
