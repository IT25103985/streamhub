import React from 'react';
import './SearchResults.css';

//tab switcher component for toggling between videos and playlists
const SearchResultsTabs = ({ activeTab = 'videos', onTabChange }) => {
  return (
    <div className="search-tabs-container">
      <div className="search-tabs-list">
        <button
          type="button"
          className={`search-tab-btn ${activeTab === 'videos' ? 'active' : ''}`}
          onClick={() => onTabChange('videos')}
        >
          Videos
        </button>
        <button
          type="button"
          className={`search-tab-btn ${activeTab === 'playlists' ? 'active' : ''}`}
          onClick={() => onTabChange('playlists')}
        >
          Playlists
        </button>
      </div>
    </div>
  );
};

export default SearchResultsTabs;
