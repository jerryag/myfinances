import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export const ProtectedRoute = ({ children, roles }) => {
    const { user, signed, loading } = useAuth();

    if (loading) {
        return <div>Carregando...</div>;
    }

    if (!signed) {
        return <Navigate to="/" />;
    }

    if (roles && !roles.includes(user.type)) {
        return <Navigate to="/home" />;
    }

    return children;
};
