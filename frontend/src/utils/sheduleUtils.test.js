import { getScheduleByType, isNotReadySchedule, filterClassesArray } from './sheduleUtils';

describe('getScheduleByType', () => {
    it('should return an empty object for any entity and semester id', () => {
        // Arrange
        const entityId = 1;
        const semesterId = 2;
        // Act
        const result = getScheduleByType(entityId, semesterId);
        // Assert
        expect(result).toBeDefined();
        expect(result).toEqual({});
    });

    it('should return an object (not undefined) when called with no arguments', () => {
        // Act
        const result = getScheduleByType();
        // Assert
        expect(result).toBeDefined();
        expect(typeof result).toBe('object');
    });
});

describe('isNotReadySchedule', () => {
    describe('when schedule is empty ({})', () => {
        it('should return true when loading is false', () => {
            // Arrange
            const schedule = {};
            const loading = false;
            // Act
            const result = isNotReadySchedule(schedule, loading);
            // Assert
            expect(result).toBe(true);
        });

        it('should return false when loading is true', () => {
            // Arrange
            const schedule = {};
            const loading = true;
            // Act
            const result = isNotReadySchedule(schedule, loading);
            // Assert
            expect(result).toBe(false);
        });
    });

    describe('when schedule is not empty', () => {
        it('should return false when loading is false', () => {
            // Arrange
            const schedule = { class1: { period: 1, teacher: 'John' } };
            const loading = false;
            // Act
            const result = isNotReadySchedule(schedule, loading);
            // Assert
            expect(result).toBe(false);
        });

        it('should return false when loading is true', () => {
            // Arrange
            const schedule = { class1: { period: 1, teacher: 'John' } };
            const loading = true;
            // Act
            const result = isNotReadySchedule(schedule, loading);
            // Assert
            expect(result).toBe(false);
        });
    });

    describe('when schedule is null', () => {
        it('should return true when loading is false', () => {
            // Arrange
            const loading = false;
            // Act
            const result = isNotReadySchedule(null, loading);
            // Assert
            expect(result).toBe(true);
        });

        it('should return false when loading is true', () => {
            // Arrange
            const loading = true;
            // Act
            const result = isNotReadySchedule(null, loading);
            // Assert
            expect(result).toBe(false);
        });
    });
});

describe('filterClassesArray', () => {
    it('should return empty array for empty input', () => {
        // Arrange
        const input = [];
        // Act
        const result = filterClassesArray(input);
        // Assert
        expect(result).toEqual([]);
    });

    it('should return all elements when all ids are unique', () => {
        // Arrange
        const input = [
            { id: 1, name: 'Math' },
            { id: 2, name: 'Physics' },
            { id: 3, name: 'Chemistry' },
        ];
        // Act
        const result = filterClassesArray(input);
        // Assert
        expect(result).toEqual(input);
        expect(result).toHaveLength(3);
    });

    it('should remove duplicate entries keeping first occurrence', () => {
        // Arrange
        const input = [
            { id: 1, name: 'Math' },
            { id: 2, name: 'Physics' },
            { id: 1, name: 'Math duplicate' },
        ];
        // Act
        const result = filterClassesArray(input);
        // Assert
        expect(result).toHaveLength(2);
        expect(result[0]).toEqual({ id: 1, name: 'Math' });
        expect(result[1]).toEqual({ id: 2, name: 'Physics' });
    });

    it('should deduplicate array where all elements have the same id', () => {
        // Arrange
        const input = [
            { id: 5, name: 'a' },
            { id: 5, name: 'b' },
            { id: 5, name: 'c' },
        ];
        // Act
        const result = filterClassesArray(input);
        // Assert
        expect(result).toHaveLength(1);
        expect(result[0]).toEqual({ id: 5, name: 'a' });
    });
});
