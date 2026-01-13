import { useAuth } from '../context/AuthContext';
import { useTitle } from '../context/PageTitleContext';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

export const Header = () => {
    const { user, logout } = useAuth();
    const title = useTitle();
    const navigate = useNavigate();
    const [isDropdownOpen, setIsDropdownOpen] = useState(false);

    const handleLogout = () => {
        logout();
        window.location.href = '/';
    };

    const handleChangePassword = () => {
        navigate('/change-password');
        setIsDropdownOpen(false);
    };

    return (
        <header style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '1rem 2rem',
            background: '#ffffff', // Or a primary color
            boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
            marginBottom: '20px'
        }}>
            <div
                onClick={() => navigate('/home')}
                style={{ fontWeight: 'bold', fontSize: '1.2rem', color: '#333', cursor: 'pointer' }}
            >
                MyFinances
            </div>

            <div style={{ fontSize: '1.2rem', fontWeight: '500', color: '#555' }}>
                {title || ''}
            </div>

            <div style={{ position: 'relative' }}>
                <div
                    onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                    style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '8px', color: '#333' }}
                >
                    <span style={{ fontWeight: '500' }}>{user?.name || user?.login}</span>
                    <span style={{ fontSize: '0.8rem' }}>▼</span>
                </div>

                {isDropdownOpen && (
                    <div style={{
                        position: 'absolute',
                        top: '120%',
                        right: 0,
                        background: 'white',
                        border: '1px solid #ddd',
                        borderRadius: '4px',
                        boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
                        minWidth: '150px',
                        zIndex: 1000
                    }}>
                        <div
                            onClick={handleChangePassword}
                            style={{ padding: '10px 15px', cursor: 'pointer', borderBottom: '1px solid #eee', color: '#333' }}
                            onMouseEnter={(e) => e.currentTarget.style.background = '#f5f5f5'}
                            onMouseLeave={(e) => e.currentTarget.style.background = 'white'}
                        >
                            Trocar Senha
                        </div>
                        <div
                            onClick={handleLogout}
                            style={{ padding: '10px 15px', cursor: 'pointer', color: '#d9534f' }}
                            onMouseEnter={(e) => e.currentTarget.style.background = '#f5f5f5'}
                            onMouseLeave={(e) => e.currentTarget.style.background = 'white'}
                        >
                            Sair
                        </div>
                    </div>
                )}
            </div>
        </header>
    );
};
