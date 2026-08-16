import React, { useState, useContext, useEffect, useRef } from 'react';
import { AuthContext } from '../context/AuthContext';

const AuthModal = () => {
  const { isAuthModalOpen, authModalMode, closeAuthModal, setAuthModalMode, login, register } = useContext(AuthContext);
  const modalRef = useRef(null);

  const [loginData, setLoginData] = useState({
    email: '',
    password: '',
    keepMeLoggedIn: false
  });

  const [registerData, setRegisterData] = useState({
    username: '',
    firstName: '',
    lastName: '',
    email: '',
    dateOfBirth: '',
    password: '',
    confirmPassword: ''
  });

  const [fieldErrors, setFieldErrors] = useState({});
  const [globalError, setGlobalError] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);

  const passwordRules = {
    length: registerData.password.length >= 8,
    uppercase: /[A-Z]/.test(registerData.password),
    lowercase: /[a-z]/.test(registerData.password),
    number: /[0-9]/.test(registerData.password),
    special: /[@$!%*?&]/.test(registerData.password)
  };

  useEffect(() => {
    setFieldErrors({});
    setGlobalError(null);
    setSuccessMsg(null);
  }, [authModalMode, isAuthModalOpen]);

  useEffect(() => {
    if (!isAuthModalOpen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        closeAuthModal();
      } else if (e.key === 'Tab') {
        if (!modalRef.current) return;
        const focusableElements = modalRef.current.querySelectorAll(
          'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled])'
        );
        if (focusableElements.length === 0) return;
        
        const firstElement = focusableElements[0];
        const lastElement = focusableElements[focusableElements.length - 1];

        if (e.shiftKey) {
          if (document.activeElement === firstElement || document.activeElement === document.body) {
            lastElement.focus();
            e.preventDefault();
          }
        } else {
          if (document.activeElement === lastElement || document.activeElement === document.body) {
            firstElement.focus();
            e.preventDefault();
          }
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    
    // Auto focus the modal to trap keyboard
    setTimeout(() => {
      if (modalRef.current) {
        modalRef.current.focus();
      }
    }, 100);

    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isAuthModalOpen, closeAuthModal]);

  if (!isAuthModalOpen) return null;

  const handleLoginChange = (e) => {
    const { name, value, type, checked } = e.target;
    setLoginData({
      ...loginData,
      [name]: type === 'checkbox' ? checked : value
    });
    setFieldErrors(prev => ({ ...prev, [name]: null }));
    setGlobalError(null);
  };

  const handleRegisterChange = (e) => {
    setRegisterData({
      ...registerData,
      [e.target.name]: e.target.value
    });
    setFieldErrors(prev => ({ ...prev, [e.target.name]: null }));
    setGlobalError(null);
  };

  const parseServerErrors = (err) => {
    const msg = err.response?.data?.message || '';
    const msgLower = msg.toLowerCase();
    let newFieldErrors = {};
    let isFieldError = false;

    if (msgLower.includes('email is already') || msgLower.includes('invalid email')) {
      newFieldErrors.email = msg;
      isFieldError = true;
    }
    if (msgLower.includes('username is already')) {
      newFieldErrors.username = msg;
      isFieldError = true;
    }
    if (msgLower.includes('age must be') || msgLower.includes('date of birth')) {
      newFieldErrors.dateOfBirth = msg;
      isFieldError = true;
    }
    if (msgLower.includes('password') && authModalMode === 'register') {
      newFieldErrors.password = msg;
      isFieldError = true;
    }

    if (err.response?.data?.errors && Array.isArray(err.response.data.errors)) {
      err.response.data.errors.forEach(e => {
        newFieldErrors[e.field] = e.defaultMessage;
        isFieldError = true;
      });
    }

    if (isFieldError) {
      setFieldErrors(newFieldErrors);
    } else {
      setGlobalError(msg || 'An unexpected error occurred. Please try again.');
    }
  };

  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    try {
      await login(loginData.email, loginData.password, loginData.keepMeLoggedIn);
      closeAuthModal();
    } catch (err) {
      parseServerErrors(err);
      if (err.response?.status === 401) {
        setGlobalError('Invalid email or password.');
      }
    }
  };

  const handleRegisterSubmit = async (e) => {
    e.preventDefault();
    
    if (registerData.password !== registerData.confirmPassword) {
      setFieldErrors({ confirmPassword: 'Passwords do not match' });
      return;
    }

    try {
      await register(registerData);
      setAuthModalMode('login'); 
      setSuccessMsg('Account created successfully! Please log in.');
      setRegisterData({
        username: '', firstName: '', lastName: '', email: '', dateOfBirth: '',
        password: '', confirmPassword: ''
      });
    } catch (err) {
      parseServerErrors(err);
    }
  };

  const switchMode = (mode) => {
    setAuthModalMode(mode);
  };

  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      closeAuthModal();
    }
  };

  return (
    <div className="modal-overlay" onClick={handleBackdropClick}>
      <div 
        className="modal-card" 
        ref={modalRef} 
        tabIndex="-1"
        role="dialog"
        aria-modal="true"
      >
        <button className="modal-close" onClick={closeAuthModal} aria-label="Close">
          ✕
        </button>

        <div style={{ textAlign: 'center', marginBottom: '30px' }}>
          <h2 style={{ fontFamily: '"Space Grotesk", sans-serif', fontSize: '28px', margin: '0 0 8px 0', color: 'var(--text-on-light)' }}>
            {authModalMode === 'login' ? 'Welcome back' : 'Create An Account'}
          </h2>
          <p style={{ color: 'var(--text-on-light-dim)', fontSize: '14px', margin: 0 }}>
            {authModalMode === 'login' ? 'Sign in to access your library' : 'Join StreamHub today'}
          </p>
        </div>

        {globalError && (
          <div style={{ background: '#fef2f2', color: '#dc2626', padding: '12px', borderRadius: '6px', marginBottom: '20px', fontSize: '13px', border: '1px solid #fecaca', textAlign: 'center' }}>
            {globalError}
          </div>
        )}

        {successMsg && (
          <div style={{ background: '#f0fdf4', color: '#16a34a', padding: '12px', borderRadius: '6px', marginBottom: '20px', fontSize: '13px', border: '1px solid #bbf7d0', textAlign: 'center' }}>
            {successMsg}
          </div>
        )}

        {authModalMode === 'login' ? (
          <form onSubmit={handleLoginSubmit}>
            <div style={{ marginBottom: '14px' }}>
              <input type="email" name="email" value={loginData.email} onChange={handleLoginChange} className="modal-input" placeholder="Email Address" required style={{ marginBottom: fieldErrors.email ? '4px' : '14px' }} />
              {fieldErrors.email && <div style={{ color: '#dc2626', fontSize: '12px', marginBottom: '10px' }}>{fieldErrors.email}</div>}
            </div>

            <div style={{ marginBottom: '14px' }}>
              <input type="password" name="password" value={loginData.password} onChange={handleLoginChange} className="modal-input" placeholder="Password" required style={{ marginBottom: fieldErrors.password ? '4px' : '14px' }} />
              {fieldErrors.password && <div style={{ color: '#dc2626', fontSize: '12px', marginBottom: '10px' }}>{fieldErrors.password}</div>}
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '24px' }}>
              <input 
                type="checkbox" 
                id="keepMeLoggedIn"
                name="keepMeLoggedIn"
                checked={loginData.keepMeLoggedIn}
                onChange={handleLoginChange}
              />
              <label htmlFor="keepMeLoggedIn" style={{ fontSize: '13px', color: 'var(--text-on-light-dim)', cursor: 'pointer' }}>Keep me logged in</label>
            </div>

            <button type="submit" className="modal-submit">Sign In</button>
          </form>
        ) : (
          <form onSubmit={handleRegisterSubmit}>
            
            <div style={{ display: 'flex', gap: '14px', marginBottom: '0' }}>
              <div style={{ flex: 1 }}>
                <input type="text" name="firstName" value={registerData.firstName} onChange={handleRegisterChange} className="modal-input" placeholder="First Name" required style={{ marginBottom: fieldErrors.firstName ? '4px' : '14px' }} />
                {fieldErrors.firstName && <div style={{ color: '#dc2626', fontSize: '12px', marginBottom: '10px' }}>{fieldErrors.firstName}</div>}
              </div>
              <div style={{ flex: 1 }}>
                <input type="text" name="lastName" value={registerData.lastName} onChange={handleRegisterChange} className="modal-input" placeholder="Last Name" required style={{ marginBottom: fieldErrors.lastName ? '4px' : '14px' }} />
                {fieldErrors.lastName && <div style={{ color: '#dc2626', fontSize: '12px', marginBottom: '10px' }}>{fieldErrors.lastName}</div>}
              </div>
            </div>

            <div style={{ marginBottom: '0' }}>
              <input type="text" name="username" value={registerData.username} onChange={handleRegisterChange} className="modal-input" placeholder="Username" required style={{ marginBottom: fieldErrors.username ? '4px' : '14px' }} />
              {fieldErrors.username && <div style={{ color: '#dc2626', fontSize: '12px', marginBottom: '10px' }}>{fieldErrors.username}</div>}
            </div>

            <div style={{ display: 'flex', gap: '14px', marginBottom: '0' }}>
              <div style={{ flex: 1 }}>
                <input type="email" name="email" value={registerData.email} onChange={handleRegisterChange} className="modal-input" placeholder="Email Address" required style={{ marginBottom: fieldErrors.email ? '4px' : '14px' }} />
                {fieldErrors.email && <div style={{ color: '#dc2626', fontSize: '12px', marginBottom: '10px' }}>{fieldErrors.email}</div>}
              </div>
              <div style={{ flex: 1 }}>
                <input type="date" name="dateOfBirth" value={registerData.dateOfBirth} onChange={handleRegisterChange} className="modal-input" required style={{ marginBottom: fieldErrors.dateOfBirth ? '4px' : '14px' }} />
                {fieldErrors.dateOfBirth && <div style={{ color: '#dc2626', fontSize: '12px', marginBottom: '10px' }}>{fieldErrors.dateOfBirth}</div>}
              </div>
            </div>

            <div style={{ marginBottom: '0' }}>
              <input type="password" name="password" value={registerData.password} onChange={handleRegisterChange} className="modal-input" placeholder="Password" required style={{ marginBottom: '8px' }} />
              {fieldErrors.password && <div style={{ color: '#dc2626', fontSize: '12px', marginBottom: '10px' }}>{fieldErrors.password}</div>}
              
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', fontSize: '11px', fontWeight: '500', marginBottom: '14px', color: 'var(--text-on-light-dim)' }}>
                <span style={{ color: passwordRules.length ? '#16a34a' : 'inherit' }}>{passwordRules.length ? '✓ ' : '○ '}8+ chars</span>
                <span style={{ color: passwordRules.uppercase ? '#16a34a' : 'inherit' }}>{passwordRules.uppercase ? '✓ ' : '○ '}Uppercase</span>
                <span style={{ color: passwordRules.lowercase ? '#16a34a' : 'inherit' }}>{passwordRules.lowercase ? '✓ ' : '○ '}Lowercase</span>
                <span style={{ color: passwordRules.number ? '#16a34a' : 'inherit' }}>{passwordRules.number ? '✓ ' : '○ '}Number</span>
                <span style={{ color: passwordRules.special ? '#16a34a' : 'inherit' }}>{passwordRules.special ? '✓ ' : '○ '}Special char</span>
              </div>
            </div>

            <div style={{ marginBottom: '0' }}>
              <input type="password" name="confirmPassword" value={registerData.confirmPassword} onChange={handleRegisterChange} className="modal-input" placeholder="Confirm Password" required style={{ marginBottom: fieldErrors.confirmPassword ? '4px' : '24px' }} />
              {fieldErrors.confirmPassword && <div style={{ color: '#dc2626', fontSize: '12px', marginBottom: '14px' }}>{fieldErrors.confirmPassword}</div>}
            </div>

            <button type="submit" className="modal-submit">Create Account</button>
          </form>
        )}

        <div style={{ marginTop: '24px', textAlign: 'center', fontSize: '13.5px', color: 'var(--text-on-light-dim)' }}>
          {authModalMode === 'login' ? (
            <span>
              New here?{' '}
              <button 
                onClick={() => switchMode('register')} 
                style={{ background: 'none', border: 'none', color: 'var(--violet)', fontWeight: '600', cursor: 'pointer', padding: 0 }}
              >
                Create an account
              </button>
            </span>
          ) : (
            <span>
              Already a member?{' '}
              <button 
                onClick={() => switchMode('login')} 
                style={{ background: 'none', border: 'none', color: 'var(--violet)', fontWeight: '600', cursor: 'pointer', padding: 0 }}
              >
                Sign in
              </button>
            </span>
          )}
        </div>

      </div>
    </div>
  );
};

export default AuthModal;
