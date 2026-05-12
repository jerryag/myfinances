import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { usePageTitle } from '../context/PageTitleContext';
import { useState } from 'react';
import { TransactionReportFilterModal } from '../components/TransactionReportFilterModal';
import { MessageModal } from '../components/MessageModal';

export const Home = () => {
    const { user } = useAuth();
    const navigate = useNavigate();
    usePageTitle('Home');
    const [showReportDropdown, setShowReportDropdown] = useState(false);
    const [isReportFilterOpen, setIsReportFilterOpen] = useState(false);
    const [messageModal, setMessageModal] = useState({ isOpen: false, title: '', message: '', type: 'info' });

    return (
        <div className="home-container">
            <main className="dashboard-grid">
                {['ADMIN', 'MASTER'].includes(user.type) && (
                    <div className="card">
                        <h3>Usuários</h3>
                        <p>Gerencie usuários e acessos.</p>
                        <button onClick={() => navigate('/users')} className="btn-card">Acessar</button>
                    </div>
                )}

                <div className="card">
                    <h3>Tipos de Transação</h3>
                    <p>Cadastre tipos de receitas e despesas.</p>
                    <button onClick={() => navigate('/transaction-types')} className="btn-card">Acessar</button>
                </div>

                <div className="card">
                    <h3>Lançamentos</h3>
                    <p>Gerencie suas receitas e despesas.</p>
                    <button onClick={() => navigate('/monthly-planning')} className="btn-card">Acessar</button>
                </div>
                <div className="card" style={{ position: 'relative' }}>
                    <h3>Relatórios</h3>
                    <p>Visualize seus gastos mensais.</p>
                    <button 
                        className="btn-card"
                        onClick={() => setShowReportDropdown(!showReportDropdown)}
                    >
                        Acessar
                    </button>
                    {showReportDropdown && (
                        <div style={{
                            position: 'absolute', top: '100%', left: 0, right: 0,
                            background: '#333', border: '1px solid #444', borderRadius: '4px',
                            zIndex: 10, marginTop: '5px'
                        }}>
                            <div 
                                style={{ padding: '10px', cursor: 'pointer', borderBottom: '1px solid #444' }}
                                onClick={() => {
                                    setShowReportDropdown(false);
                                    setIsReportFilterOpen(true);
                                }}
                            >
                                Lançamentos por mês
                            </div>
                        </div>
                    )}
                </div>
                <div className="card">
                    <h3>Perfil</h3>
                    <p>Atualize seus dados cadastrais.</p>
                    <button className="btn-card">Acessar</button>
                </div>
                <div className="card">
                    <h3>Configurações</h3>
                    <p>Preferências do sistema.</p>
                    <button className="btn-card">Acessar</button>
                </div>
            </main >

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
        </div >
    );
};
