import React from 'react';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('StreamHub caught an unhandled render error:', error, errorInfo);
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null });
    window.location.href = '/';
  };

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          minHeight: '100vh',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '24px',
          background: 'var(--void, #0a0d16)',
          color: '#fff',
          textAlign: 'center'
        }}>
          <div style={{ fontSize: '32px', fontWeight: 800, marginBottom: '8px' }}>
            Stream<span style={{ color: '#e50914' }}>Hub</span>
          </div>
          <h2 style={{ fontSize: '20px', marginBottom: '12px', color: '#ef4444' }}>
            Something went wrong loading this page
          </h2>
          {this.state.error?.message && (
            <div style={{
              background: 'rgba(239, 68, 68, 0.1)',
              border: '1px solid rgba(239, 68, 68, 0.25)',
              color: '#f87171',
              padding: '8px 16px',
              borderRadius: '6px',
              fontSize: '13px',
              marginBottom: '18px',
              fontFamily: 'monospace',
              maxWidth: '600px',
              wordBreak: 'break-word'
            }}>
              {this.state.error.message}
            </div>
          )}
          <p style={{ color: 'var(--text-dim, #94a3b8)', maxWidth: '500px', marginBottom: '24px', fontSize: '14px', lineHeight: 1.5 }}>
            An unexpected error occurred while rendering this view. You can return to the home page or try reloading.
          </p>
          <div style={{ display: 'flex', gap: '12px' }}>
            <button
              onClick={() => { this.setState({ hasError: false, error: null }); }}
              style={{
                padding: '10px 20px',
                background: 'rgba(255, 255, 255, 0.1)',
                border: '1px solid rgba(255, 255, 255, 0.2)',
                color: '#fff',
                borderRadius: '8px',
                cursor: 'pointer',
                fontWeight: 600
              }}
            >
              Try Again
            </button>
            <button
              onClick={this.handleReset}
              style={{
                padding: '10px 20px',
                background: '#e50914',
                border: 'none',
                color: '#fff',
                borderRadius: '8px',
                cursor: 'pointer',
                fontWeight: 600
              }}
            >
              Back to Home
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
