import { useState, useEffect } from 'react';
import { useParams, Link, useSearchParams } from 'react-router-dom';
import axiosInstance from '../api/axiosInstance';
import VideoCard from '../components/VideoCard';
import VideoGridSkeleton from '../components/skeletons/VideoGridSkeleton';
import VideoCardSkeleton from '../components/skeletons/VideoCardSkeleton';
import SkeletonBase from '../components/skeletons/SkeletonBase';
import VideoWatchPanel from '../features/video/VideoWatchPanel';

const PAGE_SIZE = 8;

//category browse page showing banner and filtered video grid
const CategoryPage = () => {
  const { categoryId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  
  const page = parseInt(searchParams.get('page') || '0', 10);
  const query = searchParams.get('q') || '';
  
  const [category, setCategory] = useState(null);
  const [categoryLoading, setCategoryLoading] = useState(true);
  const [categoryError, setCategoryError] = useState('');
  const [libraryVideos, setLibraryVideos] = useState([]);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [activeVideoId, setActiveVideoId] = useState(null);

  //load category details
  const fetchCategoryDetails = () => {
    setCategoryLoading(true);
    setCategoryError('');

    if (categoryId === 'trending') {
      setCategory({
         id: 'trending',
         name: 'Trending',
         description: 'What everyone on the server is watching right now.',
         slug: 'trending'
      });
      setCategoryLoading(false);
      return;
    }

    axiosInstance.get(`/categories/${categoryId}`)
      .then(res => {
        setCategory(res.data);
        setCategoryError('');
      })
      .catch(err => {
        console.error('Error fetching category', err);
        // Fallback default info so user is not blocked
        setCategory({
          id: categoryId,
          name: 'Category ' + categoryId,
          description: 'Discover videos in this category',
          slug: 'gaming'
        });
      })
      .finally(() => {
        setCategoryLoading(false);
      });
  };

  useEffect(() => {
    setCategory(null);
    setLibraryVideos([]);
    fetchCategoryDetails();
  }, [categoryId]);

  useEffect(() => {
    let isMounted = true;
    const fetchLibrary = async () => {
      setLoading(true);
      try {
        if (categoryId === 'trending') {
          const res = await axiosInstance.get('/videos/trending');
          if (!isMounted) return;
          const rawVideos = res.data || [];
          const newVideos = rawVideos.filter((v) => v && (v.viewCount || 0) >= 10);
          
          if (query) {
             const lowerQuery = query.toLowerCase();
             setLibraryVideos(newVideos.filter(v => v.title.toLowerCase().includes(lowerQuery)));
          } else {
             setLibraryVideos(newVideos);
          }
          setHasMore(false);
        } else {
          const params = { page, size: PAGE_SIZE, categoryId: categoryId };
          if (query) params.search = query;

          const res = await axiosInstance.get('/videos', { params });
          if (!isMounted) return;
          const newVideos = res.data.content || res.data || [];
          const isLast = res.data.last !== undefined ? res.data.last : newVideos.length < PAGE_SIZE;

          if (page === 0) {
            setLibraryVideos(newVideos);
          } else {
            setLibraryVideos(prev => [...prev, ...newVideos]);
          }
          setHasMore(!isLast);
        }
      } catch (err) {
        if (!isMounted) return;
        console.error('Error fetching category library', err);
        if (page === 0) {
          setLibraryVideos([]);
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    fetchLibrary();
    return () => {
      isMounted = false;
    };
  }, [categoryId, query, page]);

  const handleLoadMore = () => {
    setSearchParams({ page: (page + 1).toString(), ...(query ? { q: query } : {}) });
  };

  const styleCat = category?.slug || 'gaming';

  return (
    <>
      {categoryLoading ? (
        <section className="cat-banner gaming" style={{ minHeight: '160px' }}>
          <div className="cat-banner-inner">
            <Link to="/" className="back-link">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M19 12H5M12 19l-7-7 7-7"/>
              </svg>
              Back to Home
            </Link>
            <SkeletonBase width="240px" height="36px" style={{ margin: '8px 0' }} />
            <SkeletonBase width="400px" height="20px" />
          </div>
        </section>
      ) : (
        <section className={`cat-banner ${styleCat}`}>
          <div className="cat-banner-inner">
            <Link to="/" className="back-link">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M19 12H5M12 19l-7-7 7-7"/>
              </svg>
              Back to Home
            </Link>
            <h2>{category?.name || 'Category'}</h2>
            <p>{category?.description || 'Browse videos in this category'}</p>
          </div>
        </section>
      )}

      <section className="block" id="library">
        <div className="block-head">
          <div>
            <h3>Category library</h3>
            <p><span id="library-count">{libraryVideos.length} videos</span> currently on the server.</p>
          </div>
        </div>
        <div className="block-inner">
          {loading && libraryVideos.length === 0 ? (
            <VideoGridSkeleton count={8} id="library-grid" />
          ) : (
            <div className="videos-grid" id="library-grid">
              {libraryVideos.length ? (
                libraryVideos.map(v => <VideoCard key={v.videoId || v.id} video={v} onClick={() => setActiveVideoId(v.videoId || v.id)} />)
              ) : (
                !loading && <div className="empty-state">Nothing matches that search yet.</div>
              )}
              {loading && libraryVideos.length > 0 && (
                Array.from({ length: 4 }).map((_, i) => (
                  <VideoCardSkeleton key={`cat-more-skel-${i}`} />
                ))
              )}
            </div>
          )}
          {hasMore && (
            <div className="load-more-row">
              <button className="btn-load" onClick={handleLoadMore} disabled={!hasMore || loading}>
                Load more videos <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M6 9l6 6 6-6" /></svg>
              </button>
            </div>
          )}
        </div>
      </section>

      {activeVideoId && (
        <VideoWatchPanel 
          videoId={activeVideoId} 
          onClose={() => setActiveVideoId(null)} 
        />
      )}
    </>
  );
};

export default CategoryPage;
