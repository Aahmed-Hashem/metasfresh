const toggleFrequentFilters = () => {
  cy.clickElementWithClass('.filters-frequent .toggle-filters');
  cy.get('.filter-widget').should('exist');
};

const toggleNotFrequentFilters = () => {
  cy.clickElementWithClass('.filters-not-frequent .toggle-filters');
  cy.get('.filter-menu').should('exist');
};

const selectFrequentFilterWidget = () => {
  return cy.get('.filters-frequent .filter-widget');
};

const selectNotFrequentFilterWidget = filterId => {
  cy.clickElementWithClass(`.filter-menu .filter-option-${filterId}`, true);
  return cy.get('.filters-not-frequent .filter-widget');
};

const applyFilters = () => {
  cy.clickElementWithClass('.filter-btn-wrapper .applyBtn', true);
};

export {
  toggleFrequentFilters,
  toggleNotFrequentFilters,
  selectFrequentFilterWidget,
  selectNotFrequentFilterWidget,
  applyFilters,
};
