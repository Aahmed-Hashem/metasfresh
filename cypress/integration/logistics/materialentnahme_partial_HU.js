import { BPartner } from '../../support/utils/bpartner';
import { BPartnerLocation } from '../../support/utils/bpartner_ui';
import { DiscountSchema } from '../../support/utils/discountschema';
import { ProductCategory } from '../../support/utils/product';
import { PackingMaterial } from '../../support/utils/packing_material';
import { PackingInstructions } from '../../support/utils/packing_instructions';
import { PackingInstructionsVersion } from '../../support/utils/packing_instructions_version';
import { Builder } from '../../support/utils/builder';
import { humanReadableNow } from '../../support/utils/utils';
import { PurchaseOrder, PurchaseOrderLine } from '../../support/utils/purchase_order';
import {
  applyFilters,
  selectNotFrequentFilterWidget,
  toggleNotFrequentFilters,
  clearNotFrequentFilters,
} from '../../support/functions';

const date = humanReadableNow();
const productForPackingMaterialTU = `ProductPackingMaterialTU_${date}`;
const productForPackingMaterialLU = `ProductPackingMaterialLU_${date}`;
const packingMaterialForTU = `PackingMaterialForTU_${date}`;
const packingMaterialForLU = `PackingMaterialForLU${date}`;
const packingInstructionsNameForTU = `ProductPackingInstructionsForTU_${date}`;
const packingInstructionsNameForLU = `ProductPackingInstructionsForLU_${date}`;
const productName1 = `Product1_${date}`;
const productCategoryName = `ProductCategoryName_${date}`;
const discountSchemaName = `DiscountSchema_${date}`;
const priceSystemName = `PriceSystem_${date}`;
const priceListName = `PriceList_${date}`;
const priceListVersionName = `PriceListVersion_${date}`;
const productType = 'Item';
const vendorName = `Vendor_${date}`;
const materialentnahme = 'Materialentnahmelager';
const warehouseName = 'Hauptlager_StdWarehouse_Hauptlager_0_0_0';
const packingInstructionsVersionForTU = `PackingInstructionsVersionForTU_${date}`;
const packingInstructionsVersionForLU = `PackingInstructionsVersionForLU_${date}`;

describe('Partial material withdrawal in handling unit editor with Materialentnahmelager', function() {
  it('Create price entities', function() {
    Builder.createBasicPriceEntities(priceSystemName, priceListVersionName, priceListName, false);
    cy.fixture('discount/discountschema.json').then(discountSchemaJson => {
      Object.assign(new DiscountSchema(), discountSchemaJson)
        .setName(discountSchemaName)
        .apply();
    });
  });
  it('create product from 24_Gebinde category in order to use it for packing material for transport unit', function() {
    // eslint-disable-next-line
    Builder.createProductWithPriceUsingExistingCategory(priceListName, productForPackingMaterialTU, productForPackingMaterialTU, productType, "24_Gebinde");
  });
  it('create product from 24_Gebinde category in order to use it for packing material for loading unit', function() {
    Builder.createProductWithPriceUsingExistingCategory(
      priceListName,
      productForPackingMaterialLU,
      productForPackingMaterialLU,
      productType,
      '24_Gebinde'
    );
  });
  it('create packing material for handling unit - Transport unit', function() {
    cy.fixture('product/packing_material.json').then(packingMaterialJson => {
      Object.assign(new PackingMaterial(), packingMaterialJson)
        .setName(packingMaterialForTU)
        .setProduct(productForPackingMaterialTU)
        .setLength('0')
        .setWidth('0')
        .setHeight('0')
        .apply();
    });
  });
  it('create packing material for handling unit - Load unit', function() {
    cy.fixture('product/packing_material.json').then(packingMaterialJson => {
      Object.assign(new PackingMaterial(), packingMaterialJson)
        .setName(packingMaterialForLU)
        .setProduct(productForPackingMaterialLU)
        .setLength('0')
        .setWidth('0')
        .setHeight('0')
        .apply();
    });
  });
  it('create packing instruction for handling unit - Transport unit', function() {
    cy.fixture('product/packing_instructions.json').then(packingInstructionsJson => {
      Object.assign(new PackingInstructions(), packingInstructionsJson)
        .setName(packingInstructionsNameForTU)
        .apply();
    });
  });
  it('create packing instruction for handling unit - Load unit', function() {
    cy.fixture('product/packing_instructions.json').then(packingInstructionsJson => {
      Object.assign(new PackingInstructions(), packingInstructionsJson)
        .setName(packingInstructionsNameForLU)
        .apply();
    });
  });
  it('create packing instruction version for handling unit - Transport unit; with packgut - quantity=0.00 and packmittel', function() {
    cy.fixture('product/packing_instructions_version.json').then(pivJson => {
      Object.assign(new PackingInstructionsVersion(), pivJson)
        .setName(packingInstructionsVersionForTU)
        .setPackingInstructions(packingInstructionsNameForTU)
        .setPackingMaterial(packingMaterialForTU)
        .apply();
    });
    /**For some reasons,the quantity for the packgut cannot be added in advanced edit mode, only like below */
    cy.selectTab('M_HU_PI_Item');
    cy.selectNthRow(1)
      .find('.Quantity')
      .dblclick({ force: true })
      .find('.form-field-Qty input')
      .type('0', { force: true });
    cy.selectTab('M_HU_PI_Item');
  });
  it('create packing instruction version for handling unit - Loading unit; with packmittel and packingInstructionsNameForTU as unter-packvorschrift', function() {
    cy.fixture('product/packing_instructions_version.json').then(pivJson => {
      Object.assign(new PackingInstructionsVersion(), pivJson)
        .setName(packingInstructionsVersionForLU)
        .setPackingInstructions(packingInstructionsNameForLU)
        .setPackingMaterial(packingMaterialForLU)
        .setUnit('Load/Logistique Unit')
        .apply();
    });
    cy.selectTab('M_HU_PI_Item');
    cy.pressAddNewButton();
    cy.selectInListField('ItemType', 'Unter-Packvorschrift', true);
    /**VERY IMPORTANT!!! the packing instruction for TU have to be set as unter-packvorschrift otherwise the LU quantity won't appear after creating a material receipt */
    cy.selectInListField('Included_HU_PI_ID', packingInstructionsNameForTU, true);
    cy.writeIntoStringField('Qty', '10', true, null, true);
    cy.pressDoneButton();
  });

  it('Create category', function() {
    cy.fixture('product/simple_productCategory.json').then(productCategoryJson => {
      Object.assign(new ProductCategory(), productCategoryJson)
        .setName(productCategoryName)
        .apply();
    });
  });

  it('Create product for the purchase order', function() {
    Builder.createProductWithPriceAndCUTUAllocationUsingExistingCategory(
      productCategoryName,
      productCategoryName,
      priceListName,
      productName1,
      productName1,
      productType,
      packingInstructionsVersionForTU
    );
  });

  it('Create vendor', function() {
    new BPartner({ name: vendorName })
      .setVendor(true)
      .setVendorPricingSystem(priceSystemName)
      .setVendorDiscountSchema(discountSchemaName)
      .setPaymentTerm('30 days net')
      .addLocation(new BPartnerLocation('Address1').setCity('Cologne').setCountry('Deutschland'))
      .apply();

    cy.readAllNotifications();
  });
});

describe('Create a purchase order and Material Receipts', function() {
  it('Create a purchase order and visit Material Receipt Candidates', function() {
    new PurchaseOrder()
      .setBPartner(vendorName)
      .setPriceSystem(priceSystemName)
      .setPoReference('test')
      .addLine(new PurchaseOrderLine().setProduct(productName1).setQuantity(5))
      .apply();
    cy.completeDocument();
  });

  it('Visit referenced Material Receipt Candidates', function() {
    cy.openReferencedDocuments('M_ReceiptSchedule');
    cy.expectNumberOfRows(2);
  });

  it('Create Material Receipt', function() {
    cy.selectNthRow(0).click();
    cy.executeQuickAction('WEBUI_M_ReceiptSchedule_ReceiveHUs_UsingDefaults', false);
    cy.selectNthRow(0, true);
    // cy.get('.quick-actions-tag.pointer').should('contain', 'Create material receipt').click();
    cy.executeQuickAction('WEBUI_M_HU_CreateReceipt_NoParams', false, true, false);
    cy.pressDoneButton();
  });
//   it('Check if Materialentnahmelager warehouse exists', function() {
//     cy.visitWindow('139');
//     toggleNotFrequentFilters();
//     selectNotFrequentFilterWidget('default');
//     cy.writeIntoStringField('Name', materialentnahme, false, null, true);
//     applyFilters();

//     cy.expectNumberOfRows(1);
//   });
//   it('Partial material withdrawal in handling unit editor', function() {
//     cy.visitWindow('540189');
//     toggleNotFrequentFilters();
//     selectNotFrequentFilterWidget('default');
//     cy.writeIntoLookupListField('M_Product_ID', productName1, productName1, false, false, null, true);
//     cy.writeIntoLookupListField('M_Locator_ID', warehouseName, warehouseName, false, false, null, true);
//     applyFilters();
//   });
//   it('Select first row - related to LU quantity and extract 1 from there', function() {
    // cy.selectNthRow(0).click();
//     cy.executeQuickAction('WEBUI_M_HU_MoveTUsToDirectWarehouse', false, false, false);
//     cy.writeIntoStringField('QtyTU', '1', true, null, true);
//     cy.pressStartButton();

//     clearNotFrequentFilters();

//     toggleNotFrequentFilters();
//     selectNotFrequentFilterWidget('default');
//     cy.writeIntoLookupListField('M_Product_ID', productName1, productName1, false, false, null, true);
//     cy.writeIntoLookupListField('M_Locator_ID', warehouseName, warehouseName, false, false, null, true);
//     applyFilters();

//     cy.selectNthRow(0)
//       .find('.Quantity')
//       .should('contain', '40');
//   });
});
