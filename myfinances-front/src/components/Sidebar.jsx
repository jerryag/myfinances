import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useState } from 'react';
import { TransactionReportFilterModal } from './TransactionReportFilterModal';
import { MessageModal } from './MessageModal';
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
    
    const [showReportDropdown, setShowReportDropdown] = useState(false);
    const [isReportFilterOpen, setIsReportFilterOpen] = useState(false);
    const [messageModal, setMessageModal] = useState({ isOpen: false, title: '', message: '', type: 'info' });

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
                        style={{ position: 'relative' }}
                        onClick={() => {
                            if (item.label === 'Relatórios') {
                                setShowReportDropdown(!showReportDropdown);
                            } else if (item.path !== '#') {
                                navigate(item.path);
                            }
                        }}
                    >
                        <span className="sidebar-item-icon">{item.icon}</span>
                        <span className="sidebar-item-label">{item.label}</span>
                        {item.label === 'Relatórios' && showReportDropdown && (
                            <div style={{
                                position: 'absolute', top: '100%', left: '10px',
                                background: '#333', border: '1px solid #444', borderRadius: '4px',
                                zIndex: 100, marginTop: '5px', color: '#fff', whiteSpace: 'nowrap'
                            }}>
                                <div 
                                    style={{ padding: '10px', cursor: 'pointer' }}
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        setShowReportDropdown(false);
                                        setIsReportFilterOpen(true);
                                    }}
                                >
                                    Lançamentos por mês
                                </div>
                            </div>
                        )}
                    </li>
                ))}
            </ul>

            <TransactionReportFilterModal 
                isOpen={isReportFilterOpen}
                onClose={() => setIsReportFilterOpen(false)}
                onSuccess={() => setMessageModal({
                    isOpen: true,
                    title: 'Sucesso',
                    message: 'Relatório gerado com sucesso!',
                    type: 'success'
                })}
            />

            <MessageModal 
                isOpen={messageModal.isOpen}
                onClose={() => setMessageModal({ ...messageModal, isOpen: false })}
                title={messageModal.title}
                message={messageModal.message}
                type={messageModal.type}
            />
        </div>
    );
};
