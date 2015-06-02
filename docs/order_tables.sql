
-- view
SELECT cpo.OrderNumber, cpo.OrderReference, cpo.AccountReference AS MerchantCode, cpo.PlacedDate, cpo.CancelDate, cpo.ConfirmDate, cpo.CpConfirmDate, cpo.CpPlacedDate, cpo.HubId, cpo.Status, cpoa.FirstName, cpoa.MiddleName, cpoa.LastName, COALESCE (cpoa.Street1, '') + ' || ' + COALESCE (cpoa.Street2, '') + ' || ' + COALESCE (cpoa.Street3, '') AS Street, cpoa.City, cpoa.Region, cpoa.Country, cpoa.PostalCode, 
         COALESCE (cpoa.PhoneNumber1, '') + ' || ' + COALESCE (cpoa.PhoneNumber2, '') AS PhoneNumber, cpoa.Email, cpodc.SKU, cpodc.URL, cpodc.OrderDetailsCommodityId, cpodc.Name, cpodc.HSCode, cpodl.Quantity, cpm.Name AS MerchantName, cpma.FirstName AS SellerFirstName, cpma.MiddleName AS SellerMiddleName, cpma.LastName AS SellerLastName, COALESCE (cpma.Street1, '') + ' || ' + COALESCE (cpma.Street2, '') 
         + ' || ' + COALESCE (cpma.Street3, '') AS SellerStreet, cpma.City AS SellerCity, cpma.Region AS SellerRegion, cpma.Country AS SellerCountry, cpma.PostalCode AS SellerPostalCode, COALESCE (cpma.PhoneNumber1, '') + ' || ' + COALESCE (cpma.PhoneNumber2, '') AS SellerPhoneNumber, cpma.Email AS SellerEmail, cpp.PartnerCode, partnerAddr.Company AS PartnerName, CASE WHEN (Cpodc.URL IS NOT NULL) AND (CHARINDEX(Cpodc.URL, 
         'www.ebay.com/itm') <> 0) THEN REVERSE(SUBSTRING(LTRIM(RTRIM(REVERSE(cpodc.URL))), 1, CHARINDEX('/', LTRIM(RTRIM(REVERSE(cpodc.URL))) - 1))) ELSE '' END AS EBAYITEMID
FROM  dbo.cpOrder AS cpo WITH (NOLOCK) INNER JOIN
         dbo.cpOrderDetails AS cpod WITH (NOLOCK) ON cpo.OrderDetailsId = cpod.OrderDetailsId INNER JOIN
         dbo.cpOrderConsignee AS cpoc WITH (NOLOCK) ON cpod.OrderConsigneeId = cpoc.OrderConsigneeId INNER JOIN
         dbo.cpOrderAddress AS cpoa WITH (NOLOCK) ON cpoc.AddressId = cpoa.AddressId INNER JOIN
         dbo.cpOrderDetailsLine AS cpodl WITH (NOLOCK) ON cpo.OrderDetailsId = cpodl.OrderDetailsId INNER JOIN
         dbo.cpOrderDetailsCommodity AS cpodc WITH (NOLOCK) ON cpodl.OrderDetailsCommodityId = cpodc.OrderDetailsCommodityId INNER JOIN
         dbo.cpMerchant AS cpm WITH (NOLOCK) ON cpo.AccountReference = cpm.MerchantCode AND cpm.PartnerCode = cpo.PartnerCode INNER JOIN
         dbo.cpMerchantAddress AS cpma WITH (NOLOCK) ON cpm.AddressId = cpma.AddressId INNER JOIN
         dbo.cpPartner AS cpp WITH (NOLOCK) ON cpm.PartnerCode = cpp.PartnerCode INNER JOIN
         dbo.cpPartnerAddress AS partnerAddr WITH (NOLOCK) ON cpp.AddressId = partnerAddr.AddressId

{"cpOrder", "cpOrderDetails", "cpOrderConsignee", "cpOrderAddress",
"cpOrderDetailsLine", "cpOrderDetailsCommodity",
"cpMerchant", "cpMerchantAddress", "cpPartner", "cpPartnerAddress",
"cpParcel", "cpInboundParcel", "cpExceptionParcel"}
		 
/*
cpOrder (order)
 (1) -> cpOrderDetails (order)
	(1) -> cpOrderConsignee (order)
		(1) -> cpOrderAddress (consigneeAddress)
--
	(m) -> cpOrderDetailsLine (orderLine)
	    (1) -> cpOrderDetailsCommodity (orderLine)
-- LATER: MUST BE A VIEW THROUGH cpMerchantCategory
		(0:1) -> cpMerchantCommodity () ON orderLine.SKU and merchant.merchantCode
			   (0:m) -> cpMerchantCommodityIdTypes 
--
 (1) -> cpMerchant (merchant)
	(1) -> cpMerchantAddress (merchantAddress)
	(1) -> cpPartner (partner)
		(1) -> cpPartnerAddress (partnerAddress)
--
 (0:m) -> cpParcel (parcel)
	(0:1) -> ParcelCentral.HubApp.LicensePlate
 (0:m) -> cpInboundParcel (inboundParcel)
 (0:m) -> cpExceptionParcel (exceptionParcel)
*/
