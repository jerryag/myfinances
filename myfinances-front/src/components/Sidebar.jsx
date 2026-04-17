import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { 
    FaBars, 
    FaHome, 
    FaUsers, 
    FaExchangeAlt, 
    FaListAlt, 
    FaChartPie, 
    FaUser, 
    FaCog 
} from 'react-icons/fa';

export const Sidebar = () => {
    const { user } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    const menuItems = [
        { path: '/home', label: 'Home', icon: <FaHome />, roles: null },
        { path: '/users', label: 'Usuários', icon: <FaUsers />, roles: ['ADMIN', 'MASTER'] },
        { path: '/transaction-types', label: 'Tipos de Transação', icon: <FaExchangeAlt />, roles: null },
        { path: '/monthly-planning', label: 'Lançamentos', icon: <FaListAlt />, roles: null },
        { path: '#', label: 'Relatórios', icon: <FaChartPie />, roles: null },
        { path: '#', label: 'Perfil', icon: <FaUser />, roles: null },
        { path: '#', label: 'Configurações', icon: <FaCog />, roles: null }
    ];

    const hasAccess = (roles) => {
        if (!roles) return true;
        return user && roles.includes(user.type);
    };

    return (
        <div className="sidebar">
            <div className="sidebar-header">
                <FaBars className="sidebar-icon" />
            </div>
            <ul className="sidebar-menu">
                {menuItems.filter(item => hasAccess(item.roles)).map((item, index) => (
                    <li 
                        key={index} 
                        className={`sidebar-item ${location.pathname === item.path ? 'active' : ''}`}
                        onClick={() => item.path !== '#' && navigate(item.path)}
                    >
                        <span className="sidebar-item-icon">{item.icon}</span>
                        <span className="sidebar-item-label">{item.label}</span>
                    </li>
                ))}
            </ul>
        </div>
    );
};
