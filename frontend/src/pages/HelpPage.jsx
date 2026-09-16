import React, { useState, useEffect } from 'react';
import { helpApi } from '../api/helpApi';
import './HelpPage.css';

//returns an icon corresponding to the section category
const getSectionIcon = (sectionName = '') => {
  const s = sectionName.toLowerCase();
  if (s.includes('account') || s.includes('start') || s.includes('profile')) {
    return (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
        <circle cx="12" cy="7" r="4" />
      </svg>
    );
  }
  if (s.includes('video') || s.includes('upload')) {
    return (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <polygon points="23 7 16 12 23 17 23 7" />
        <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
      </svg>
    );
  }
  if (s.includes('categor')) {
    return (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
      </svg>
    );
  }
  if (s.includes('playlist')) {
    return (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <line x1="8" y1="6" x2="21" y2="6" />
        <line x1="8" y1="12" x2="21" y2="12" />
        <line x1="8" y1="18" x2="21" y2="18" />
        <line x1="3" y1="6" x2="3.01" y2="6" />
        <line x1="3" y1="12" x2="3.01" y2="12" />
        <line x1="3" y1="18" x2="3.01" y2="18" />
      </svg>
    );
  }
  if (s.includes('comment') || s.includes('like')) {
    return (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
      </svg>
    );
  }
  if (s.includes('report')) {
    return (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
        <line x1="12" y1="9" x2="12" y2="13" />
        <line x1="12" y1="17" x2="12.01" y2="17" />
      </svg>
    );
  }
  if (s.includes('history') || s.includes('recommend')) {
    return (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="12" cy="12" r="10" />
        <polyline points="12 6 12 12 16 14" />
      </svg>
    );
  }
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" />
      <line x1="12" y1="16" x2="12" y2="12" />
      <line x1="12" y1="8" x2="12.01" y2="8" />
    </svg>
  );
};

//public help center page rendering backend managed topics in accordion cards
const HelpPage = () => {
  const [sections, setSections] = useState([]);
  const [openSectionTitle, setOpenSectionTitle] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchTopics();
  }, []);

  const fetchTopics = async () => {
    setLoading(true);
    try {
      const data = await helpApi.getAllTopics();
      
      //group flat list of topics by section name
      const groupedMap = new Map();
      (data || []).forEach(item => {
        const sec = item.section || 'General';
        if (!groupedMap.has(sec)) {
          groupedMap.set(sec, []);
        }
        groupedMap.get(sec).push(item);
      });

      const groupedList = Array.from(groupedMap.entries()).map(([sectionTitle, items]) => ({
        title: sectionTitle,
        icon: getSectionIcon(sectionTitle),
        entries: items.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0))
      }));

      setSections(groupedList);
      if (groupedList.length > 0 && !openSectionTitle) {
        setOpenSectionTitle(groupedList[0].title);
      }
    } catch (err) {
      console.error('Failed to load help topics', err);
    } finally {
      setLoading(false);
    }
  };

  //toggle accordion card open state
  const handleToggle = (title) => {
    setOpenSectionTitle(prev => (prev === title ? null : title));
  };

  return (
    <div className="help-page">
      <div className="help-header">
        <span className="help-badge">Help Center</span>
        <h1 className="help-title">Frequently Asked Questions</h1>
        <p className="help-subtitle">
          Find quick answers about accounts, videos, playlists, community interactions, and moderation.
        </p>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-dim)' }}>
          Loading help topics...
        </div>
      ) : sections.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-dim)' }}>
          No help topics found.
        </div>
      ) : (
        <div className="help-accordion-list">
          {sections.map((section) => {
            const isOpen = openSectionTitle === section.title;
            return (
              <div 
                key={section.title} 
                className={`help-topic-card ${isOpen ? 'open' : ''}`}
              >
                <button
                  type="button"
                  className="help-topic-header"
                  onClick={() => handleToggle(section.title)}
                  aria-expanded={isOpen}
                >
                  <div className="help-topic-title-row">
                    <div className="help-topic-icon">
                      {section.icon}
                    </div>
                    <span>{section.title}</span>
                  </div>
                  <svg 
                    className="help-chevron" 
                    width="18" 
                    height="18" 
                    viewBox="0 0 24 24" 
                    fill="none" 
                    stroke="currentColor" 
                    strokeWidth="2.5" 
                    strokeLinecap="round" 
                    strokeLinejoin="round"
                  >
                    <polyline points="6 9 12 15 18 9" />
                  </svg>
                </button>

                {isOpen && (
                  <div className="help-topic-body">
                    {section.entries.map((entry) => (
                      <div key={entry.id || entry.question} className="help-qa-item">
                        <h3 className="help-question">{entry.question}</h3>
                        <p className="help-answer">{entry.answer}</p>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default HelpPage;
