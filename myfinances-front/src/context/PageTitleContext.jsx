import { createContext, useState, useContext, useEffect } from 'react';

const PageTitleContext = createContext();

export const PageTitleProvider = ({ children }) => {
    const [title, setTitle] = useState('');

    return (
        <PageTitleContext.Provider value={{ title, setTitle }}>
            {children}
        </PageTitleContext.Provider>
    );
};

export const usePageTitle = (newTitle) => {
    const { setTitle } = useContext(PageTitleContext);

    useEffect(() => {
        if (newTitle) {
            setTitle(newTitle);
        }
    }, [newTitle, setTitle]);

    return { setTitle };
};

export const useTitle = () => {
    const { title } = useContext(PageTitleContext);
    return title;
}
