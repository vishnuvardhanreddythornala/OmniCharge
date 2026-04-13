import { downloadCSV } from './csv-export.util';

describe('downloadCSV', () => {
  let appendChildSpy: jasmine.Spy;
  let removeChildSpy: jasmine.Spy;
  let createObjectURLSpy: jasmine.Spy;
  let clickSpy: jasmine.Spy;

  beforeEach(() => {
    clickSpy = jasmine.createSpy('click');
    // Mock document.createElement to capture the <a> element
    spyOn(document, 'createElement').and.returnValue({
      setAttribute: jasmine.createSpy('setAttribute'),
      click: clickSpy,
      style: {}
    } as any);
    appendChildSpy = spyOn(document.body, 'appendChild');
    removeChildSpy = spyOn(document.body, 'removeChild');
    createObjectURLSpy = spyOn(URL, 'createObjectURL').and.returnValue('blob:mock');
  });

  it('should do nothing for empty array', () => {
    downloadCSV([], 'test');
    expect(document.createElement).not.toHaveBeenCalled();
  });

  it('should do nothing for null/undefined data', () => {
    downloadCSV(null as any, 'test');
    expect(document.createElement).not.toHaveBeenCalled();
  });

  it('should generate CSV and trigger download', () => {
    const data = [
      { name: 'Alice', age: 30 },
      { name: 'Bob', age: 25 }
    ];
    
    downloadCSV(data, 'users');
    
    expect(document.createElement).toHaveBeenCalledWith('a');
    expect(createObjectURLSpy).toHaveBeenCalled();
    
    const link = (document.createElement as jasmine.Spy).calls.mostRecent().returnValue;
    expect(link.setAttribute).toHaveBeenCalledWith('href', 'blob:mock');
    expect(link.setAttribute).toHaveBeenCalledWith('download', jasmine.stringMatching(/^users-\d{4}-\d{2}-\d{2}\.csv$/));
    
    expect(appendChildSpy).toHaveBeenCalled();
    expect(clickSpy).toHaveBeenCalled();
    expect(removeChildSpy).toHaveBeenCalled();
  });

  it('should handle rows with empty/null values gracefully', () => {
    const data = [
      { name: null, value: undefined, status: '' }
    ];
    
    downloadCSV(data, 'edge');
    expect(document.createElement).toHaveBeenCalledWith('a');
    expect(createObjectURLSpy).toHaveBeenCalled();
  });
});
