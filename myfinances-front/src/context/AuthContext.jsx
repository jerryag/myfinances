import { createContext, useState, useContext, useEffect } from 'react';
import { api } from '../services/api';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const storedUser = localStorage.getItem('user');
        if (storedUser) {
            setUser(JSON.parse(storedUser));
        }
        setLoading(false);
    }, []);

    const login = async (login, password) => {
        const params = new URLSearchParams();
        params.append('login', login);
        params.append('password', password);

        const response = await api.post('/login', params);
        const userData = response.data;

        localStorage.setItem('user', JSON.stringify(userData));
        setUser(userData);
        return userData;
    };

    const logout = () => {
        localStorage.removeItem('user');
        setUser(null);
    };

    const changePassword = async (userId, oldPassword, newPassword) => {
        const params = new URLSearchParams();
        params.append('userId', userId);
        params.append('oldPassword', oldPassword);
        params.append('newPassword', newPassword);

        await api.post('/users/change-password', params);
    };

    return (
        <AuthContext.Provider value={{ user, signed: !!user, login, logout, changePassword, loading }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    return useContext(AuthContext);
};
