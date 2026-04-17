import { Outlet } from 'react-router-dom';
import { Header } from './Header';
import { Sidebar } from './Sidebar';
import { PageTitleProvider } from '../context/PageTitleContext';

export const Layout = () => {
    return (
        <PageTitleProvider>
            <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
                <Header />
                <div style={{ display: 'flex', flex: 1, position: 'relative' }}>
                    <Sidebar />
                    <main style={{ flex: 1, padding: '0 2rem 2rem 4rem' }}>
                        <Outlet />
                    </main>
                </div>
            </div>
        </PageTitleProvider>
    );
};
