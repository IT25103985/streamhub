import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import axiosInstance from '../api/axiosInstance';
import VideoCard from '../components/VideoCard';
import CategoryCard from '../components/CategoryCard';
import Chip from '../components/Chip';

const PAGE_SIZE = 8;

const HomePage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  
  const query = searchParams.get('q') || '';
  const categoryFilter = searchParams.get('category') || 'all';
  const page = parseInt(searchParams.get('page') || '0', 10);

  const [categories, setCategories] = useState([]);
  const [libraryVideos, setLibraryVideos] = useState([]);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  
  // Fetch categories for the homepage blocks and chips
  useEffect(() => {
    axiosInstance.get('/categories')
      .then(res => setCategories(res.data))
      .catch(err => console.error('Error fetching categories', err));
  }, []);

  // Fetch library grid
  useEffect(() => {
    const fetchLibrary = async () => {
      setLoading(true);
      try {
        if (categoryFilter === 'trending') {
          const res = await axiosInstance.get('/videos/trending');
          const newVideos = res.data;
          
          if (query) {
             const lowerQuery = query.toLowerCase();
             setLibraryVideos(newVideos.filter(v => v.title.toLowerCase().includes(lowerQuery)));
          } else {
             setLibraryVideos(newVideos);
          }
          setHasMore(false);
        } else {
          const params = { page, size: PAGE_SIZE };
          if (query) params.search = query;
          if (categoryFilter !== 'all') params.category = categoryFilter;

          const res = await axiosInstance.get('/videos', { params });
          const newVideos = res.data.content || res.data; // Handle Page vs List
          const isLast = res.data.last !== undefined ? res.data.last : newVideos.length < PAGE_SIZE;

          if (page === 0) {
            setLibraryVideos(newVideos);
          } else {
            setLibraryVideos(prev => [...prev, ...newVideos]);
          }
          setHasMore(!isLast);
        }
      } catch (err) {
        console.error('Error fetching library', err);
      } finally {
        setLoading(false);
      }
    };

    fetchLibrary();
  }, [query, categoryFilter, page]);

  const handleFilter = (cat) => {
    setSearchParams({ category: cat, page: '0', ...(query ? { q: query } : {}) });
  };

  const handleLoadMore = () => {
    setSearchParams({ category: categoryFilter, page: (page + 1).toString(), ...(query ? { q: query } : {}) });
  };

  const displayCategories = categories.length >= 3 ? categories.slice(0, 3) : [
    { id: 1, slug: 'gaming', name: 'Gaming', description: 'Playthroughs, speedruns & ranked grinds', icon: '🎮', bgUrl: 'https://images.unsplash.com/photo-1542751371-adc38448a05e?q=80&w=800&auto=format&fit=crop' },
    { id: 2, slug: 'music', name: 'Music', description: 'Live sets, lo-fi sessions & studio takes', icon: '🎵', bgUrl: 'https://images.unsplash.com/photo-1516280440614-37939bbacd81?q=80&w=800&auto=format&fit=crop' },
    { id: 3, slug: 'movies', name: 'Movies ❤️', description: 'Slow-burn romance & rainy-day dramas', icon: '🎬', bgUrl: 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?q=80&w=800&auto=format&fit=crop' }
  ];

  const trendingCat = { id: 'trending', slug: 'trending', name: 'Trending', description: 'What everyone is watching right now', icon: '🔥', bgUrl: 'https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=800&auto=format&fit=crop' };
  
  const allCards = [trendingCat, ...displayCategories].slice(0, 4);
  const allChips = [trendingCat, ...categories];

  return (
    <>
      <section className="hero">
        <div className="hero-inner">
          <h2>Unlimited entertainment,<br /><em>tuned to your mood.</em></h2>
          <div className="hero-cta">
            <a href="#library" className="btn btn-primary">Browse library</a>
          </div>
        </div>
      </section>

      <section className="block">
        <div className="block-head">
          <div>
            <h3>Browse by category</h3>
            <p>Pick a lane, or scroll down for everything at once.</p>
          </div>
        </div>
        <div className="block-inner">
          <div className="cards">
            {allCards.map(cat => (
              <CategoryCard 
                key={cat.id} 
                categoryId={cat.id} 
                slug={cat.slug || cat.name.toLowerCase()} 
                title={cat.name} 
                description={cat.description || 'Explore this category'}
                icon={cat.icon || '📺'}
                bgUrl={cat.bgUrl || 'https://images.unsplash.com/photo-1542751371-adc38448a05e?q=80&w=800&auto=format&fit=crop'} 
              />
            ))}
          </div>
        </div>
      </section>

      {/* Trending section removed */}

      <section className="block" id="library">
        <div className="block-head">
          <div>
            <h3>Full library</h3>
            <p><span id="library-count">{libraryVideos.length} videos</span> currently on the server — filter by category or search by title.</p>
          </div>
        </div>
        <div className="block-inner">
          <div className="chip-row">
            <Chip active={categoryFilter === 'all'} onClick={handleFilter} filter="all" label="All" />
            {allChips.map(cat => (
               <Chip 
                 key={cat.id} 
                 active={categoryFilter === cat.id.toString()} 
                 onClick={handleFilter} 
                 filter={cat.id.toString()} 
                 label={`${cat.icon || ''} ${cat.name}`.trim()} 
               />
            ))}
            {/* Fallbacks if no categories fetched yet */}
            {categories.length === 0 && (
              <>
                <Chip active={categoryFilter === '1'} onClick={handleFilter} filter="1" label="🎮 Gaming" />
                <Chip active={categoryFilter === '2'} onClick={handleFilter} filter="2" label="🎵 Music" />
                <Chip active={categoryFilter === '3'} onClick={handleFilter} filter="3" label="🎬 Movies" />
              </>
            )}
          </div>
          <div className="videos-grid" id="library-grid">
            {libraryVideos.length ? (
              libraryVideos.map(v => <VideoCard key={v.id} video={v} />)
            ) : (
              !loading && <div className="empty-state"><span>🛰️</span>Nothing matches that search yet.</div>
            )}
          </div>
          <div className="load-more-row">
            <button className="btn-load" id="load-more-btn" onClick={handleLoadMore} disabled={!hasMore || loading}>
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

export default HomePage;
