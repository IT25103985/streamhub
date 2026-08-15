import { useState, useEffect } from 'react';
import { useParams, Link, useSearchParams } from 'react-router-dom';
import axiosInstance from '../api/axiosInstance';
import VideoCard from '../components/VideoCard';
import Chip from '../components/Chip';

const PAGE_SIZE = 8;

const CategoryPage = () => {
  const { categoryId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  
  const page = parseInt(searchParams.get('page') || '0', 10);
  const query = searchParams.get('q') || '';
  
  const [category, setCategory] = useState(null);
  const [libraryVideos, setLibraryVideos] = useState([]);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);

  // Fetch category details
  useEffect(() => {
    // Attempt to fetch single category
    axiosInstance.get(`/categories/${categoryId}`)
      .then(res => setCategory(res.data))
      .catch(err => {
        console.error('Error fetching category', err);
        // Fallback for demo
        setCategory({
           id: categoryId,
           name: 'Category ' + categoryId,
           description: 'Discover videos in this category',
           slug: 'gaming' // mock slug to get right colors
        });
      });
  }, [categoryId]);

  // Fetch library grid for this category
  useEffect(() => {
    const fetchLibrary = async () => {
      setLoading(true);
      try {
        const params = { page, size: PAGE_SIZE, category: categoryId };
        if (query) params.search = query;

        const res = await axiosInstance.get('/videos', { params });
        const newVideos = res.data.content || res.data;
        const isLast = res.data.last !== undefined ? res.data.last : newVideos.length < PAGE_SIZE;

        if (page === 0) {
          setLibraryVideos(newVideos);
        } else {
          setLibraryVideos(prev => [...prev, ...newVideos]);
        }
        setHasMore(!isLast);
      } catch (err) {
        console.error('Error fetching category library', err);
      } finally {
        setLoading(false);
      }
    };

    fetchLibrary();
  }, [categoryId, query, page]);

  const handleLoadMore = () => {
    setSearchParams({ page: (page + 1).toString(), ...(query ? { q: query } : {}) });
  };

  if (!category) return null;
  
  const styleCat = category.slug || 'gaming';

  return (
    <>
      <section className={`cat-banner ${styleCat}`}>
        <div className="cat-banner-inner">
          <Link to="/" className="back-link">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M19 12H5M12 19l-7-7 7-7"/>
            </svg>
            Back to Home
          </Link>
          <h2>{category.name}</h2>
          <p>{category.description}</p>
        </div>
      </section>

      <section className="block" id="library">
        <div className="block-head">
          <div>
            <h3>Category library</h3>
            <p><span id="library-count">{libraryVideos.length} videos</span> currently on the server.</p>
          </div>
        </div>
        <div className="block-inner">
          <div className="videos-grid" id="library-grid">
            {libraryVideos.length ? (
              libraryVideos.map(v => <VideoCard key={v.id} video={v} />)
            ) : (
              !loading && <div className="empty-state"><span>🛰️</span>Nothing matches that search yet.</div>
            )}
          </div>
          <div className="load-more-row">
            <button className="btn-load" onClick={handleLoadMore} disabled={!hasMore || loading}>
              {hasMore ? (
                <>Load more videos <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M6 9l6 6 6-6" /></svg></>
              ) : (
                "You've reached the end of the library"
              )}
            </button>
          </div>
        </div>
      </section>
    </>
  );
};

export default CategoryPage;
