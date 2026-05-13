import React from 'react';
import { render, screen } from '@testing-library/react';
import { getHref } from './getHref';

jest.mock('../i18n', () => ({
    __esModule: true,
    default: { t: (key) => key },
}));

describe('getHref function', () => {
    it('should return link with href', () => {
        const link = 'https://www.youtube.com/';
        render(getHref(link));
        const anchor = screen.getByTitle(link);
        expect(anchor).toBeInTheDocument();
        expect(anchor).toHaveAttribute('href', link);
    });

    describe('edge cases', () => {
        it('should handle empty string link', () => {
            // Arrange
            const link = '';
            // Act
            render(getHref(link));
            const anchor = screen.getByTitle(link);
            // Assert
            expect(anchor).toBeInTheDocument();
            expect(anchor).toHaveAttribute('href', link);
        });

        it('should render anchor element when link is null', () => {
            // Arrange & Act
            const { container } = render(getHref(null));
            // Assert
            const anchor = container.querySelector('a');
            expect(anchor).toBeInTheDocument();
        });

        it('should handle link without protocol', () => {
            // Arrange
            const link = 'www.example.com';
            // Act
            render(getHref(link));
            const anchor = screen.getByTitle(link);
            // Assert
            expect(anchor).toBeInTheDocument();
            expect(anchor).toHaveAttribute('href', link);
        });

        it('should handle very long link', () => {
            // Arrange
            const longLink = 'https://www.example.com/' + 'a'.repeat(2000);
            // Act
            render(getHref(longLink));
            const anchor = screen.getByTitle(longLink);
            // Assert
            expect(anchor).toBeInTheDocument();
            expect(anchor).toHaveAttribute('href', longLink);
        });
    });

    describe('element attributes', () => {
        it('should have className "link-to-meeting"', () => {
            // Arrange
            const link = 'https://www.example.com/';
            // Act
            render(getHref(link));
            const anchor = screen.getByTitle(link);
            // Assert
            expect(anchor).toHaveClass('link-to-meeting');
        });

        it('should open link in a new tab with target="_blank"', () => {
            // Arrange
            const link = 'https://www.example.com/';
            // Act
            render(getHref(link));
            const anchor = screen.getByTitle(link);
            // Assert
            expect(anchor).toHaveAttribute('target', '_blank');
        });

        it('should have rel="noreferrer" for security', () => {
            // Arrange
            const link = 'https://www.example.com/';
            // Act
            render(getHref(link));
            const anchor = screen.getByTitle(link);
            // Assert
            expect(anchor).toHaveAttribute('rel', 'noreferrer');
        });
    });
});