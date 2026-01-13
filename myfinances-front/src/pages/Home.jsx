import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

export const Home = () => {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/');
    };

    return (
        <div className="home-container">
            <header className="home-header">
                <h1>Bem-vindo, {user?.name}</h1>
                <button onClick={handleLogout} className="btn-logout">Sair</button>
            </header>

            <main className="dashboard-grid">
                <div className="home-content" style={{ gridColumn: '1 / -1', marginBottom: '20px' }}>
                    <p>Bem-vindo, {user.name}!</p>
                    {['ADMIN', 'MASTER'].includes(user.type) && (
                        <button onClick={() => navigate('/users')} className="btn-primary" style={{ marginTop: '10px' }}>
                            Gerenciar Usuários
                        </button>
                    )}
                </div>

                <div className="card">
                    <h3>Lançamentos</h3>
                    <p>Gerencie suas receitas e despesas.</p>
                    <button className="btn-card">Acessar</button>
                </div>
                <div className="card">
                    <h3>Relatórios</h3>
                    <p>Visualize seus gastos mensais.</p>
                    <button className="btn-card">Acessar</button>
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
        </div >
    );
};
