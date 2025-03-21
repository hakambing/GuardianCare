const CheckIn = require('../models/CheckIn');
const User = require('../models/User');
const mongoose = require('mongoose');
const { formatInTimeZone } = require('date-fns-tz');

const DEFAULT_TIMEZONE = process.env.DEFAULT_TIMEZONE || 'Asia/Singapore';

const convertToTimezone = (date, timezone = DEFAULT_TIMEZONE) => {
    if (!date) return null;
    const newDate = new Date(date);
    const formattedDate = formatInTimeZone(newDate, DEFAULT_TIMEZONE, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    return new Date(formattedDate);
};
const convertFromTimezoneToUTC = (date, timezone = DEFAULT_TIMEZONE) => {
    if (!date) return null;
    const localDate = new Date(date);
    const tzOffsetMinutes = new Date(formatInTimeZone(
        new Date(), 
        timezone, 
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    )).getTimezoneOffset() - new Date().getTimezoneOffset();
    return new Date(localDate.getTime() - tzOffsetMinutes * 60000);
};
const logDateWithTimezone = (date, label = 'Date') => {
    if (!date) return;

    console.log(`${label} (UTC): ${date.toISOString()}`);
    console.log(`${label} (${DEFAULT_TIMEZONE}): ${formatInTimeZone(date, DEFAULT_TIMEZONE, 'yyyy-MM-dd HH:mm:ss')}`);
};

class CheckInService {
    async getAllCheckIns() {
        try {
            const allCheckIns = await CheckIn.find({})
                .sort({ created_at: -1 })
                .lean();

            console.log(`Found ${allCheckIns.length} check-ins in the database`);

            allCheckIns.forEach(checkIn => {
                if (checkIn.created_at) {
                    checkIn.created_at_local = formatInTimeZone(
                        checkIn.created_at,
                        DEFAULT_TIMEZONE,
                        'yyyy-MM-dd HH:mm:ss'
                    );
                }

                if (checkIn.updated_at) {
                    checkIn.updated_at_local = formatInTimeZone(
                        checkIn.updated_at,
                        DEFAULT_TIMEZONE,
                        'yyyy-MM-dd HH:mm:ss'
                    );
                }
            });

            return allCheckIns;
        } catch (error) {
            console.error(`Error in getAllCheckIns: ${error.message}`);
            throw new Error(`Failed to get all check-ins: ${error.message}`);
        }
    }
    
    async getLatestCheckIn(elderlyId) {
        try {
            const elderlyIdStr = elderlyId.toString().trim();
            console.log(`Looking for latest check-in with elderly_id: ${elderlyIdStr}`);
            let latestCheckIn = await CheckIn.findOne({ elderly_id: elderlyIdStr })
                .sort({ created_at: -1 })
                .lean();

            if (!latestCheckIn) {
                console.log(`No exact match found, trying case-insensitive match for: ${elderlyIdStr}`);
                latestCheckIn = await CheckIn.findOne({
                    elderly_id: { $regex: new RegExp('^' + elderlyIdStr + '$', 'i') }
                })
                    .sort({ created_at: -1 })
                    .lean();
            }

            if (!latestCheckIn) {
                console.log(`No check-in found for elderly: ${elderlyIdStr} after all attempts`);
                throw new Error('No check-in data found for this elderly user');
            }

            console.log(`Found latest check-in for elderly: ${elderlyIdStr}`);

            if (latestCheckIn.created_at) {
                logDateWithTimezone(latestCheckIn.created_at, 'Check-in created_at');
                latestCheckIn.created_at_local = formatInTimeZone(
                    latestCheckIn.created_at,
                    DEFAULT_TIMEZONE,
                    'yyyy-MM-dd HH:mm:ss'
                );
            }

            if (latestCheckIn.updated_at) {
                latestCheckIn.updated_at_local = formatInTimeZone(
                    latestCheckIn.updated_at,
                    DEFAULT_TIMEZONE,
                    'yyyy-MM-dd HH:mm:ss'
                );
            }

            return latestCheckIn;
        } catch (error) {
            console.error(`Error in getLatestCheckIn: ${error.message}`);
            throw new Error(`Failed to get latest check-in: ${error.message}`);
        }
    }

    async getAllElderlyCheckIns(elderlyId) {
        try {
            const elderlyIdStr = elderlyId.toString().trim();

            console.log(`Looking for check-ins with elderly_id: ${elderlyIdStr}`);

            let checkIns = await CheckIn.find({ elderly_id: elderlyIdStr })
                .sort({ created_at: -1 })
                .lean();


            if (checkIns.length === 0) {
                console.log(`No exact matches found, trying case-insensitive match for: ${elderlyIdStr}`);
                checkIns = await CheckIn.find({
                    elderly_id: { $regex: new RegExp('^' + elderlyIdStr + '$', 'i') }
                })
                    .sort({ created_at: -1 })
                    .lean();
            }

            console.log(`Found ${checkIns.length} check-ins for elderly: ${elderlyIdStr}`);

            checkIns.forEach(checkIn => {
                if (checkIn.created_at) {
                    checkIn.created_at_local = formatInTimeZone(
                        checkIn.created_at,
                        DEFAULT_TIMEZONE,
                        'yyyy-MM-dd HH:mm:ss'
                    );
                }

                if (checkIn.updated_at) {
                    checkIn.updated_at_local = formatInTimeZone(
                        checkIn.updated_at,
                        DEFAULT_TIMEZONE,
                        'yyyy-MM-dd HH:mm:ss'
                    );
                }
            });

            return checkIns;
        } catch (error) {
            console.error(`Error in getAllElderlyCheckIns: ${error.message}`);
            throw new Error(`Failed to get check-ins: ${error.message}`);
        }
    }

    /**
     * Get check-ins by date range
     */
    async getElderlyCheckInsByDateRange(elderlyId, startDate, endDate) {
        try {
            const elderlyIdStr = elderlyId.toString().trim();

            const startLocal = new Date(startDate);
            const endLocal = new Date(endDate);

            if (isNaN(startLocal.getTime()) || isNaN(endLocal.getTime())) {
                throw new Error('Invalid date format');
            }

            const startUTC = convertFromTimezoneToUTC(startLocal);
            const endUTC = convertFromTimezoneToUTC(endLocal);

            console.log(`Date range (UTC): ${startUTC.toISOString()} to ${endUTC.toISOString()}`);
            console.log(`Date range (${DEFAULT_TIMEZONE}): ${formatInTimeZone(startLocal, DEFAULT_TIMEZONE, 'yyyy-MM-dd HH:mm:ss')} to ${formatInTimeZone(endLocal, DEFAULT_TIMEZONE, 'yyyy-MM-dd HH:mm:ss')}`);

            let checkIns = await CheckIn.find({
                elderly_id: elderlyIdStr,
                created_at: {
                    $gte: startUTC,
                    $lte: endUTC
                }
            })
                .sort({ created_at: -1 })
                .lean();


            if (checkIns.length === 0) {
                console.log(`No exact matches found, trying case-insensitive match for: ${elderlyIdStr}`);
                checkIns = await CheckIn.find({
                    elderly_id: { $regex: new RegExp('^' + elderlyIdStr + '$', 'i') },
                    created_at: {
                        $gte: startUTC,
                        $lte: endUTC
                    }
                })
                    .sort({ created_at: -1 })
                    .lean();
            }

            console.log(`Found ${checkIns.length} check-ins for elderly: ${elderlyIdStr} in date range`);

            checkIns.forEach(checkIn => {
                if (checkIn.created_at) {
                    checkIn.created_at_local = formatInTimeZone(
                        checkIn.created_at,
                        DEFAULT_TIMEZONE,
                        'yyyy-MM-dd HH:mm:ss'
                    );
                }

                if (checkIn.updated_at) {
                    checkIn.updated_at_local = formatInTimeZone(
                        checkIn.updated_at,
                        DEFAULT_TIMEZONE,
                        'yyyy-MM-dd HH:mm:ss'
                    );
                }
            });

            return checkIns;
        } catch (error) {
            console.error(`Error in getElderlyCheckInsByDateRange: ${error.message}`);
            throw new Error(`Failed to get check-ins by date range: ${error.message}`);
        }
    }

    async createCheckIn(checkInData) {
        try {
            if (checkInData.elderly_id) {
                checkInData.elderly_id = checkInData.elderly_id.toString().trim();
            } else {
                throw new Error('elderly_id is required');
            }

            let checkInDate = new Date();
            let startOfDay = new Date(checkInDate.getFullYear(), checkInDate.getMonth(), checkInDate.getDate(), 0, 0, 0);
            let endOfDay = new Date(checkInDate.getFullYear(), checkInDate.getMonth(), checkInDate.getDate(), 23, 59, 59);
            if (checkInData.created_at) {
                checkInDate = new Date(checkInData.created_at);
                if (isNaN(checkInDate.getTime())) {
                    throw new Error('Invalid created_at date format');
                }
                checkInDate = convertToTimezone(checkInDate, DEFAULT_TIMEZONE);
                startOfDay = new Date(checkInDate.getFullYear(), checkInDate.getMonth(), checkInDate.getDate(), 0, 0, 0);
                endOfDay = new Date(checkInDate.getFullYear(), checkInDate.getMonth(), checkInDate.getDate(), 23, 59, 59);
            }

            const existingCheckIn = await CheckIn.findOne({
                elderly_id: checkInData.elderly_id,
                created_at: {
                    $gte: startOfDay,
                    $lte: endOfDay
                }
            });

            if (existingCheckIn) {
                console.log(`Found existing check-in for elderly: ${checkInData.elderly_id} on ${startOfDay.toISOString().split('T')[0]}, updating it`);
                Object.assign(existingCheckIn, checkInData);
                existingCheckIn.created_at = checkInDate;
                existingCheckIn.updated_at = checkInDate;

                await existingCheckIn.save();

                return existingCheckIn;
            } else {
                console.log(`No existing check-in found for elderly: ${checkInData.elderly_id} on ${startOfDay.toISOString().split('T')[0]}, creating new one`);
                const newCheckIn = new CheckIn({
                    ...checkInData,
                    created_at: checkInDate,
                    updated_at: checkInDate
                });

                await newCheckIn.save();

                return newCheckIn;
            }
        } catch (error) {
            console.error(`Error in createCheckIn: ${error.message}`);
            throw new Error(`Failed to create check-in: ${error.message}`);
        }
    }

    async getCaretakerElderliesSummary(caretakerId, latestDate) {
        try {
            console.log(`Looking for elderlies belonging to caretaker ${caretakerId} for summary`);

            let searchDateLocal = new Date(), searchDateUTC = new Date();
            if (latestDate) {
                searchDateLocal = new Date(latestDate);
                searchDateUTC = convertFromTimezoneToUTC(searchDateLocal);
            }

            if (isNaN(searchDateLocal.getTime()) || isNaN(searchDateUTC.getTime())) {
                throw new Error('Invalid date format');
            }

            const elderlies = await User.find({ 
                caretaker_id: caretakerId,
                user_type: 'elderly'
            }).lean();

            console.log(`Found ${elderlies.length} elderlies assigned to caretaker ${caretakerId}`);

            const startOfDayLocal = new Date(searchDateLocal);
            startOfDayLocal.setHours(0, 0, 0, 0);
            const endOfDayLocal = new Date(searchDateLocal);
            endOfDayLocal.setHours(23, 59, 59, 999);

            const startOfDayUTC = convertFromTimezoneToUTC(startOfDayLocal);
            const endOfDayUTC = convertFromTimezoneToUTC(endOfDayLocal);

            const elderlySummaries = await Promise.all(elderlies.map(async (elderly) => {
                const elderlyId = elderly._id.toString();
                
                let checkIn = await CheckIn.findOne({
                    elderly_id: elderlyId,
                    created_at: {
                        $gte: startOfDayUTC,
                        $lte: endOfDayUTC
                    }
                }).sort({ created_at: -1 }).lean();
                
                let checkInAvailableForLatestDate = true;
                
                if (!checkIn) {
                    checkIn = await CheckIn.findOne({
                        elderly_id: elderlyId
                    }).sort({ created_at: -1 }).lean();
                    checkInAvailableForLatestDate = false;
                }
                
                if (checkIn) {
                    if (checkIn.created_at) {
                        checkIn.created_at_local = formatInTimeZone(
                            checkIn.created_at,
                            DEFAULT_TIMEZONE,
                            'yyyy-MM-dd HH:mm:ss'
                        );
                    }

                    if (checkIn.updated_at) {
                        checkIn.updated_at_local = formatInTimeZone(
                            checkIn.updated_at,
                            DEFAULT_TIMEZONE,
                            'yyyy-MM-dd HH:mm:ss'
                        );
                    }
                }
                
                return {
                    elderly: elderly,
                    checkIn: checkIn,
                    checkInAvailableForLatestDate: checkInAvailableForLatestDate
                };
            }));
            
            return elderlySummaries;
        } catch (error) {
            console.error(`Error in getCaretakerElderliesSummary: ${error.message}`);
            throw new Error(`Failed to get summary for caretaker: ${error.message}`);
        }
    }
}

module.exports = new CheckInService();
